package com.wenji.ai;

import com.wenji.plan.StudyPlan;
import com.wenji.resource.CultureResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class AiRouteValidator {

    public AiRouteDraft validate(AiRouteDraft draft, StudyPlan plan, Map<Long, CultureResource> candidates,
                                 AiRouteRequest request) {
        if (draft == null || !StringUtils.hasText(draft.title()) || !StringUtils.hasText(draft.summary())) {
            throw new IllegalArgumentException("AI 路线缺少标题或摘要");
        }
        if (draft.estimatedBudget() == null || draft.estimatedBudget().signum() < 0) {
            throw new IllegalArgumentException("AI 路线预算必须为非负数");
        }
        if (draft.days() == null || draft.days().isEmpty()) {
            throw new IllegalArgumentException("AI 路线没有包含任何日期");
        }

        Set<Long> usedResourceIds = new HashSet<>();
        Map<LocalDate, List<AiRouteDraft.Item>> normalizedDays = new TreeMap<>();
        Map<LocalDate, List<TimeRange>> occupied = new HashMap<>();
        int desiredPlaces = request.resolvedDesiredPlaces();

        for (AiRouteDraft.Day day : draft.days()) {
            if (day == null || day.date() == null || day.date().isBefore(plan.getStartDate())
                    || day.date().isAfter(plan.getEndDate())) {
                throw new IllegalArgumentException("AI 路线日期超出计划范围");
            }
            if (day.items() == null) {
                continue;
            }
            for (AiRouteDraft.Item item : day.items()) {
                if (usedResourceIds.size() >= desiredPlaces) {
                    break;
                }
                validateItem(item, day.date(), candidates, request, occupied);
                if (!usedResourceIds.add(item.resourceId())) {
                    continue;
                }
                AiRouteDraft.Item normalized = new AiRouteDraft.Item(item.resourceId(), item.startTime(),
                        item.endTime(), limit(item.transportation(), 50), limit(item.reason(), 255));
                normalizedDays.computeIfAbsent(day.date(), ignored -> new ArrayList<>()).add(normalized);
                occupied.computeIfAbsent(day.date(), ignored -> new ArrayList<>())
                        .add(new TimeRange(item.startTime(), item.endTime()));
            }
        }

        if (usedResourceIds.isEmpty()) {
            throw new IllegalArgumentException("AI 路线没有有效地点");
        }
        List<AiRouteDraft.Day> days = normalizedDays.entrySet().stream()
                .map(entry -> new AiRouteDraft.Day(entry.getKey(), entry.getValue()))
                .toList();
        List<String> tips = draft.tips() == null ? List.of() : draft.tips().stream()
                .filter(StringUtils::hasText)
                .map(tip -> limit(tip, 255))
                .limit(5)
                .toList();
        return new AiRouteDraft(limit(draft.title(), 100), limit(draft.summary(), 500),
                draft.estimatedBudget(), days, tips);
    }

    private void validateItem(AiRouteDraft.Item item, LocalDate date, Map<Long, CultureResource> candidates,
                              AiRouteRequest request, Map<LocalDate, List<TimeRange>> occupied) {
        if (item == null || item.resourceId() == null || !candidates.containsKey(item.resourceId())) {
            throw new IllegalArgumentException("AI 路线包含不存在或未发布的文化资源");
        }
        CultureResource resource = candidates.get(item.resourceId());
        if (!"PUBLISHED".equals(resource.getStatus())) {
            throw new IllegalArgumentException("AI 路线包含未发布的文化资源");
        }
        LocalTime start = item.startTime();
        LocalTime end = item.endTime();
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("AI 路线节点时间不合法");
        }
        if (start.isBefore(request.resolvedStartTime()) || end.isAfter(request.resolvedEndTime())) {
            throw new IllegalArgumentException("AI 路线节点超出每日可用时间");
        }
        boolean overlaps = occupied.getOrDefault(date, List.of()).stream()
                .anyMatch(range -> start.isBefore(range.end()) && end.isAfter(range.start()));
        if (overlaps) {
            throw new IllegalArgumentException("AI 路线同一天存在时间重叠");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
