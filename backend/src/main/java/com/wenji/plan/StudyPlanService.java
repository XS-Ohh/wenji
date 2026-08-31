package com.wenji.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.resource.CultureResource;
import com.wenji.resource.CultureResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudyPlanService {

    private final StudyPlanMapper planMapper;
    private final PlanItemMapper itemMapper;
    private final CultureResourceMapper resourceMapper;

    public StudyPlanService(StudyPlanMapper planMapper, PlanItemMapper itemMapper,
                            CultureResourceMapper resourceMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.resourceMapper = resourceMapper;
    }

    @Transactional
    public PlanResponse create(Long userId, PlanUpsertRequest request) {
        validatePlan(request);
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        copy(request, plan);
        plan.setStatus("DRAFT");
        plan.setProgress(0);
        plan.setAiGenerated(0);
        planMapper.insert(plan);
        return response(plan, List.of());
    }

    public List<PlanResponse> list(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getUpdatedAt)
                        .orderByDesc(StudyPlan::getId))
                .stream().map(this::detailResponse).toList();
    }

    public PlanResponse detail(Long userId, Long planId) {
        return detailResponse(requireOwnedPlan(userId, planId));
    }

    @Transactional
    public PlanResponse update(Long userId, Long planId, PlanUpsertRequest request) {
        validatePlan(request);
        StudyPlan plan = requireOwnedPlan(userId, planId);
        copy(request, plan);
        planMapper.updateById(plan);
        return detailResponse(planMapper.selectById(planId));
    }

    @Transactional
    public void delete(Long userId, Long planId) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        itemMapper.delete(new LambdaQueryWrapper<PlanItem>().eq(PlanItem::getPlanId, planId));
        planMapper.deleteById(plan.getId());
    }

    @Transactional
    public PlanResponse changeStatus(Long userId, Long planId, String status) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        plan.setStatus(status);
        if ("COMPLETED".equals(status)) {
            plan.setProgress(100);
        } else if ("DRAFT".equals(status)) {
            plan.setProgress(0);
        }
        planMapper.updateById(plan);
        return detailResponse(planMapper.selectById(planId));
    }

    @Transactional
    public PlanItemResponse addItem(Long userId, Long planId, PlanItemUpsertRequest request) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        CultureResource resource = requirePublishedResource(request.resourceId());
        validateItem(plan, request);

        long itemCount = itemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId));
        PlanItem item = new PlanItem();
        item.setPlanId(planId);
        copy(request, item);
        item.setSortOrder(request.sortOrder() == null ? Math.toIntExact(itemCount + 1) : request.sortOrder());
        item.setStatus("PENDING");
        itemMapper.insert(item);
        normalizeOrder(planId);
        return PlanItemResponse.from(itemMapper.selectById(item.getId()), resource);
    }

    @Transactional
    public PlanItemResponse updateItem(Long userId, Long planId, Long itemId, PlanItemUpsertRequest request) {
        StudyPlan plan = requireOwnedPlan(userId, planId);
        PlanItem item = requirePlanItem(planId, itemId);
        CultureResource resource = requirePublishedResource(request.resourceId());
        validateItem(plan, request);
        copy(request, item);
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        itemMapper.updateById(item);
        normalizeOrder(planId);
        return PlanItemResponse.from(itemMapper.selectById(itemId), resource);
    }

    @Transactional
    public void deleteItem(Long userId, Long planId, Long itemId) {
        requireOwnedPlan(userId, planId);
        PlanItem item = requirePlanItem(planId, itemId);
        itemMapper.deleteById(item.getId());
        normalizeOrder(planId);
    }

    @Transactional
    public List<PlanItemResponse> reorder(Long userId, Long planId, List<Long> itemIds) {
        requireOwnedPlan(userId, planId);
        List<PlanItem> current = items(planId);
        if (itemIds.size() != current.size() || new HashSet<>(itemIds).size() != itemIds.size()
                || !new HashSet<>(itemIds).equals(current.stream().map(PlanItem::getId).collect(Collectors.toSet()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "排序节点必须完整且属于当前计划");
        }
        Map<Long, PlanItem> byId = current.stream().collect(Collectors.toMap(PlanItem::getId, Function.identity()));
        for (int index = 0; index < itemIds.size(); index++) {
            PlanItem item = byId.get(itemIds.get(index));
            item.setSortOrder(index + 1);
            itemMapper.updateById(item);
        }
        return itemResponses(items(planId));
    }

    private PlanResponse detailResponse(StudyPlan plan) {
        List<PlanItemResponse> itemResponses = itemResponses(items(plan.getId()));
        return response(plan, itemResponses);
    }

    private PlanResponse response(StudyPlan plan, List<PlanItemResponse> items) {
        return PlanResponse.from(plan, items);
    }

    private List<PlanItem> items(Long planId) {
        return itemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .orderByAsc(PlanItem::getSortOrder)
                .orderByAsc(PlanItem::getId));
    }

    private List<PlanItemResponse> itemResponses(List<PlanItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> resourceIds = items.stream().map(PlanItem::getResourceId).distinct().toList();
        Map<Long, CultureResource> resources = resourceMapper.selectBatchIds(resourceIds).stream()
                .collect(Collectors.toMap(CultureResource::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        return items.stream().map(item -> PlanItemResponse.from(item, resources.get(item.getResourceId()))).toList();
    }

    private void normalizeOrder(Long planId) {
        List<PlanItem> ordered = items(planId);
        for (int index = 0; index < ordered.size(); index++) {
            PlanItem item = ordered.get(index);
            int expected = index + 1;
            if (item.getSortOrder() != expected) {
                item.setSortOrder(expected);
                itemMapper.updateById(item);
            }
        }
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

    private PlanItem requirePlanItem(Long planId, Long itemId) {
        PlanItem item = itemMapper.selectOne(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getId, itemId)
                .eq(PlanItem::getPlanId, planId));
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "路线节点不存在");
        }
        return item;
    }

    private CultureResource requirePublishedResource(Long resourceId) {
        CultureResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !"PUBLISHED".equals(resource.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文化资源不存在或未发布");
        }
        return resource;
    }

    private void validatePlan(PlanUpsertRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }

    private void validateItem(StudyPlan plan, PlanItemUpsertRequest request) {
        if (request.visitDate() != null && (request.visitDate().isBefore(plan.getStartDate())
                || request.visitDate().isAfter(plan.getEndDate()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "参观日期必须在计划日期范围内");
        }
        if (request.startTime() != null && request.endTime() != null
                && !request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "节点开始时间必须早于结束时间");
        }
    }

    private void copy(PlanUpsertRequest request, StudyPlan target) {
        target.setTitle(request.title());
        target.setCity(request.city());
        target.setStartDate(request.startDate());
        target.setEndDate(request.endDate());
        target.setBudget(request.budget());
        target.setInterests(request.interests());
        target.setStartLocation(request.startLocation());
    }

    private void copy(PlanItemUpsertRequest request, PlanItem target) {
        target.setResourceId(request.resourceId());
        target.setVisitDate(request.visitDate());
        target.setStartTime(request.startTime());
        target.setEndTime(request.endTime());
        target.setTransportation(request.transportation());
        target.setReason(request.reason());
    }
}
