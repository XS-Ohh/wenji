package com.wenji.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.plan.PlanItem;
import com.wenji.plan.PlanItemMapper;
import com.wenji.plan.StudyPlan;
import com.wenji.plan.StudyPlanMapper;
import com.wenji.plan.StudyPlanService;
import com.wenji.resource.CultureCategory;
import com.wenji.resource.CultureCategoryMapper;
import com.wenji.resource.CultureResource;
import com.wenji.resource.CultureResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiRouteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiRouteService.class);
    private static final int ROUTE_GAP_MINUTES = 30;

    private final StudyPlanMapper planMapper;
    private final PlanItemMapper itemMapper;
    private final CultureResourceMapper resourceMapper;
    private final CultureCategoryMapper categoryMapper;
    private final AiRouteLogMapper logMapper;
    private final StudyPlanService planService;
    private final AiRouteClient client;
    private final AiRouteValidator validator;
    private final AiRouteProperties properties;
    private final ObjectMapper objectMapper;

    public AiRouteService(StudyPlanMapper planMapper, PlanItemMapper itemMapper,
                          CultureResourceMapper resourceMapper, CultureCategoryMapper categoryMapper,
                          AiRouteLogMapper logMapper, StudyPlanService planService, AiRouteClient client,
                          AiRouteValidator validator, AiRouteProperties properties, ObjectMapper objectMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.resourceMapper = resourceMapper;
        this.categoryMapper = categoryMapper;
        this.logMapper = logMapper;
        this.planService = planService;
        this.client = client;
        this.validator = validator;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiRouteResponse generate(Long userId, Long planId, AiRouteRequest request) {
        long startedAt = System.nanoTime();
        validateDailyWindow(request);
        StudyPlan plan = requireOwnedPlan(userId, planId);
        List<CultureResource> candidates = candidates(plan);
        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前城市暂无可用于生成路线的文化资源");
        }
        Map<Long, CultureResource> candidatesById = candidates.stream()
                .collect(Collectors.toMap(CultureResource::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));

        String requestJson = routeContextJson(plan, request, candidates);
        AiRouteDraft route = null;
        Exception lastFailure = null;
        boolean fallback = true;

        if (properties.isConfigured()) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    String rawOutput = client.generate(buildPrompt(requestJson));
                    AiRouteDraft parsed = objectMapper.readValue(extractJson(rawOutput), AiRouteDraft.class);
                    route = validator.validate(parsed, plan, candidatesById, request);
                    fallback = false;
                    break;
                } catch (Exception exception) {
                    lastFailure = exception;
                }
            }
        }

        if (route == null) {
            route = fallback(plan, request, candidates);
            if (lastFailure != null) {
                LOGGER.warn("AI route generation failed for plan {}, using rule fallback: {}",
                        planId, safeMessage(lastFailure));
            }
        }

        replacePlanItems(plan, route);
        writeLog(userId, planId, requestJson, route, fallback, lastFailure, startedAt);
        return AiRouteResponse.from(route, fallback, planService.detail(userId, planId));
    }

    private List<CultureResource> candidates(StudyPlan plan) {
        List<CultureResource> resources = resourceMapper.selectList(new LambdaQueryWrapper<CultureResource>()
                .eq(CultureResource::getStatus, "PUBLISHED")
                .eq(CultureResource::getCity, plan.getCity()));
        Comparator<CultureResource> popularity = Comparator
                .comparing((CultureResource resource) -> intValue(resource.getFavoriteCount()),
                        Comparator.reverseOrder())
                .thenComparing(resource -> decimalValue(resource.getAverageRating()), Comparator.reverseOrder())
                .thenComparing(CultureResource::getId);
        resources.sort(popularity);

        Set<Long> preferredCategoryIds = preferredCategoryIds(plan.getInterests());
        if (preferredCategoryIds.isEmpty()) {
            return resources;
        }
        List<CultureResource> preferred = resources.stream()
                .filter(resource -> preferredCategoryIds.contains(resource.getCategoryId()))
                .toList();
        List<CultureResource> remaining = resources.stream()
                .filter(resource -> !preferredCategoryIds.contains(resource.getCategoryId()))
                .toList();
        List<CultureResource> ordered = new ArrayList<>(preferred.size() + remaining.size());
        ordered.addAll(preferred);
        ordered.addAll(remaining);
        return ordered;
    }

    private Set<Long> preferredCategoryIds(String interests) {
        if (interests == null || interests.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : interests.toLowerCase(Locale.ROOT).split("[,，、;；\\s]+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<CultureCategory>()
                        .eq(CultureCategory::getStatus, 1))
                .stream()
                .filter(category -> tokens.stream().anyMatch(token -> {
                    String categoryName = category.getName().toLowerCase(Locale.ROOT);
                    return token.contains(categoryName) || categoryName.contains(token);
                }))
                .map(CultureCategory::getId)
                .collect(Collectors.toSet());
    }

    private AiRouteDraft fallback(StudyPlan plan, AiRouteRequest request, List<CultureResource> candidates) {
        int target = Math.min(request.resolvedDesiredPlaces(), candidates.size());
        LocalDate date = plan.getStartDate();
        int dailyStartMinute = request.resolvedStartTime().toSecondOfDay() / 60;
        int dailyEndMinute = request.resolvedEndTime().toSecondOfDay() / 60;
        int cursorMinute = dailyStartMinute;
        Map<LocalDate, List<AiRouteDraft.Item>> scheduled = new LinkedHashMap<>();

        for (CultureResource resource : candidates.subList(0, target)) {
            int availableMinutes = dailyEndMinute - dailyStartMinute;
            int durationMinutes = Math.min(Math.max(30, intValue(resource.getRecommendedMinutes())), availableMinutes);
            if (cursorMinute + durationMinutes > dailyEndMinute) {
                date = date.plusDays(1);
                cursorMinute = dailyStartMinute;
            }
            if (date.isAfter(plan.getEndDate())) {
                break;
            }
            LocalTime startTime = LocalTime.ofSecondOfDay(cursorMinute * 60L);
            LocalTime endTime = LocalTime.ofSecondOfDay((cursorMinute + durationMinutes) * 60L);
            AiRouteDraft.Item item = new AiRouteDraft.Item(resource.getId(), startTime, endTime,
                    "步行或公共交通", "结合兴趣方向与资源热度推荐");
            scheduled.computeIfAbsent(date, ignored -> new ArrayList<>()).add(item);
            cursorMinute += durationMinutes + ROUTE_GAP_MINUTES;
        }

        if (scheduled.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每日可用时间不足以安排文化资源");
        }
        List<AiRouteDraft.Day> days = scheduled.entrySet().stream()
                .map(entry -> new AiRouteDraft.Day(entry.getKey(), entry.getValue()))
                .toList();
        BigDecimal estimatedBudget = plan.getBudget() == null ? BigDecimal.ZERO : plan.getBudget();
        return new AiRouteDraft(plan.getCity() + "文化研学路线", "根据兴趣方向、收藏热度与评分生成的路线",
                estimatedBudget, days, List.of("出发前请再次确认场馆开放时间与预约要求"));
    }

    private void replacePlanItems(StudyPlan plan, AiRouteDraft route) {
        itemMapper.delete(new LambdaQueryWrapper<PlanItem>().eq(PlanItem::getPlanId, plan.getId()));
        int sortOrder = 1;
        for (AiRouteDraft.Day day : route.days()) {
            for (AiRouteDraft.Item generated : day.items()) {
                PlanItem item = new PlanItem();
                item.setPlanId(plan.getId());
                item.setResourceId(generated.resourceId());
                item.setVisitDate(day.date());
                item.setStartTime(generated.startTime());
                item.setEndTime(generated.endTime());
                item.setSortOrder(sortOrder++);
                item.setTransportation(generated.transportation());
                item.setReason(generated.reason());
                item.setStatus("PENDING");
                itemMapper.insert(item);
            }
        }
        plan.setAiGenerated(1);
        planMapper.updateById(plan);
    }

    private StudyPlan requireOwnedPlan(Long userId, Long planId) {
        StudyPlan plan = planMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, planId)
                .eq(StudyPlan::getUserId, userId));
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研学计划不存在");
        }
        return plan;
    }

    private void validateDailyWindow(AiRouteRequest request) {
        if (!request.resolvedStartTime().isBefore(request.resolvedEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每日开始时间必须早于结束时间");
        }
        if (Duration.between(request.resolvedStartTime(), request.resolvedEndTime()).toMinutes() < 60) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "每日可用时间不能少于 60 分钟");
        }
    }

    private String routeContextJson(StudyPlan plan, AiRouteRequest request, List<CultureResource> candidates) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("city", plan.getCity());
        context.put("startDate", plan.getStartDate());
        context.put("endDate", plan.getEndDate());
        context.put("budget", plan.getBudget());
        context.put("startLocation", plan.getStartLocation());
        context.put("interests", plan.getInterests());
        context.put("desiredPlaces", request.resolvedDesiredPlaces());
        context.put("dailyStartTime", request.resolvedStartTime());
        context.put("dailyEndTime", request.resolvedEndTime());
        context.put("candidates", candidates.stream().map(this::candidateView).toList());
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法构建 AI 路线请求", exception);
        }
    }

    private Map<String, Object> candidateView(CultureResource resource) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("resourceId", resource.getId());
        view.put("name", resource.getName());
        view.put("address", resource.getAddress());
        view.put("summary", resource.getSummary());
        view.put("recommendedMinutes", resource.getRecommendedMinutes());
        view.put("favoriteCount", resource.getFavoriteCount());
        view.put("averageRating", resource.getAverageRating());
        return view;
    }

    private String buildPrompt(String contextJson) {
        return "请根据以下计划和候选资源生成文化研学路线。resourceId 只能使用 candidates 中的值：\n" + contextJson;
    }

    private String extractJson(String output) {
        if (output == null) {
            throw new IllegalArgumentException("AI 服务返回空内容");
        }
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI 服务未返回 JSON 对象");
        }
        return output.substring(start, end + 1);
    }

    private void writeLog(Long userId, Long planId, String requestJson, AiRouteDraft route, boolean fallback,
                          Exception failure, long startedAt) {
        AiRouteLog log = new AiRouteLog();
        log.setUserId(userId);
        log.setPlanId(planId);
        log.setRequestJson(requestJson);
        try {
            log.setResponseJson(objectMapper.writeValueAsString(route));
        } catch (JsonProcessingException exception) {
            log.setResponseJson(null);
        }
        log.setProvider(fallback ? "RULE_ENGINE" : "OPENAI_COMPATIBLE");
        log.setModelName(fallback ? null : properties.getModel());
        log.setStatus(fallback ? "FALLBACK" : "SUCCESS");
        log.setErrorMessage(failure == null ? null : limit(safeMessage(failure), 500));
        long durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        log.setDurationMs((int) Math.min(Integer.MAX_VALUE, durationMs));
        logMapper.insert(log);
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal decimalValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
