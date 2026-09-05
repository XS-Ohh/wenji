package com.wenji.checkin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.plan.PlanItem;
import com.wenji.plan.PlanItemMapper;
import com.wenji.plan.StudyPlan;
import com.wenji.plan.StudyPlanMapper;
import com.wenji.resource.CultureResource;
import com.wenji.resource.CultureResourceMapper;
import com.wenji.user.User;
import com.wenji.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckinService {

    private static final int CHECKIN_POINTS = 10;

    private final CheckinMapper checkinMapper;
    private final UserMapper userMapper;
    private final StudyPlanMapper planMapper;
    private final PlanItemMapper itemMapper;
    private final CultureResourceMapper resourceMapper;

    public CheckinService(CheckinMapper checkinMapper, UserMapper userMapper, StudyPlanMapper planMapper,
                          PlanItemMapper itemMapper, CultureResourceMapper resourceMapper) {
        this.checkinMapper = checkinMapper;
        this.userMapper = userMapper;
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.resourceMapper = resourceMapper;
    }

    @Transactional
    public CheckinResponse create(Long userId, CheckinCreateRequest request) {
        validateContent(request);
        requirePublishedResource(request.resourceId());
        if (request.planId() != null) {
            requirePlanResource(userId, request.planId(), request.resourceId());
        }

        Checkin existing = checkinMapper.selectOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .eq(Checkin::getResourceId, request.resourceId()));
        if (existing != null && !"REJECTED".equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该地点已提交过打卡，请勿重复提交");
        }

        LocalDateTime now = LocalDateTime.now();
        Checkin checkin = existing == null ? new Checkin() : existing;
        checkin.setUserId(userId);
        checkin.setPlanId(request.planId());
        checkin.setResourceId(request.resourceId());
        checkin.setCheckinTime(now);
        checkin.setImageUrl(trimToNull(request.imageUrl()));
        checkin.setContent(trimToNull(request.content()));
        checkin.setStatus("PENDING");
        checkin.setAuditComment(null);
        checkin.setAuditedBy(null);
        checkin.setAuditedAt(null);
        if (existing == null) {
            checkinMapper.insert(checkin);
        } else {
            checkinMapper.update(null, new LambdaUpdateWrapper<Checkin>()
                    .eq(Checkin::getId, existing.getId())
                    .eq(Checkin::getStatus, "REJECTED")
                    .set(Checkin::getPlanId, request.planId())
                    .set(Checkin::getCheckinTime, now)
                    .set(Checkin::getImageUrl, trimToNull(request.imageUrl()))
                    .set(Checkin::getContent, trimToNull(request.content()))
                    .set(Checkin::getStatus, "PENDING")
                    .set(Checkin::getAuditComment, null)
                    .set(Checkin::getAuditedBy, null)
                    .set(Checkin::getAuditedAt, null));
        }
        return response(checkinMapper.selectById(checkin.getId()));
    }

    public List<CheckinResponse> listMine(Long userId) {
        return responses(checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCreatedAt)
                .orderByDesc(Checkin::getId)));
    }

    public List<CheckinResponse> listForReview(String status) {
        LambdaQueryWrapper<Checkin> query = new LambdaQueryWrapper<Checkin>()
                .orderByAsc(Checkin::getCreatedAt)
                .orderByAsc(Checkin::getId);
        if (StringUtils.hasText(status)) {
            if (!List.of("PENDING", "APPROVED", "REJECTED").contains(status)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的打卡状态");
            }
            query.eq(Checkin::getStatus, status);
        }
        return responses(checkinMapper.selectList(query));
    }

    @Transactional
    public CheckinResponse review(Long auditorId, Long checkinId, CheckinReviewRequest request) {
        if ("REJECTED".equals(request.status()) && !StringUtils.hasText(request.auditComment())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回打卡时必须填写审核意见");
        }

        LocalDateTime now = LocalDateTime.now();
        int changed = checkinMapper.update(null, new LambdaUpdateWrapper<Checkin>()
                .eq(Checkin::getId, checkinId)
                .eq(Checkin::getStatus, "PENDING")
                .set(Checkin::getStatus, request.status())
                .set(Checkin::getAuditComment, trimToNull(request.auditComment()))
                .set(Checkin::getAuditedBy, auditorId)
                .set(Checkin::getAuditedAt, now));

        Checkin checkin = checkinMapper.selectById(checkinId);
        if (checkin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "打卡记录不存在");
        }
        if (changed == 0) {
            if (Objects.equals(checkin.getStatus(), request.status())) {
                return response(checkin);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "该打卡已经完成审核，不能变更审核结果");
        }

        if ("APPROVED".equals(request.status())) {
            userMapper.addPoints(checkin.getUserId(), CHECKIN_POINTS);
            if (checkin.getPlanId() != null) {
                completePlanResource(checkin.getPlanId(), checkin.getResourceId());
            }
        }
        return response(checkinMapper.selectById(checkinId));
    }

    private void validateContent(CheckinCreateRequest request) {
        if (!StringUtils.hasText(request.imageUrl()) && !StringUtils.hasText(request.content())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传打卡图片或填写打卡文字");
        }
        if (StringUtils.hasText(request.imageUrl())
                && !request.imageUrl().matches("^/api/uploads/images/[a-f0-9-]+\\.(jpg|png|webp)$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "打卡图片地址无效");
        }
    }

    private void requirePublishedResource(Long resourceId) {
        CultureResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !"PUBLISHED".equals(resource.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文化资源不存在或未发布");
        }
    }

    private void requirePlanResource(Long userId, Long planId, Long resourceId) {
        StudyPlan plan = planMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, planId)
                .eq(StudyPlan::getUserId, userId));
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "研学计划不存在");
        }
        long count = itemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .eq(PlanItem::getResourceId, resourceId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该文化地点不属于所选计划");
        }
    }

    private void completePlanResource(Long planId, Long resourceId) {
        itemMapper.update(null, new LambdaUpdateWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .eq(PlanItem::getResourceId, resourceId)
                .set(PlanItem::getStatus, "COMPLETED"));
        long total = itemMapper.selectCount(new LambdaQueryWrapper<PlanItem>().eq(PlanItem::getPlanId, planId));
        long completed = itemMapper.selectCount(new LambdaQueryWrapper<PlanItem>()
                .eq(PlanItem::getPlanId, planId)
                .eq(PlanItem::getStatus, "COMPLETED"));
        StudyPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setProgress(total == 0 ? 0 : Math.toIntExact(completed * 100 / total));
            if (plan.getProgress() == 100 && !"CANCELLED".equals(plan.getStatus())) {
                plan.setStatus("COMPLETED");
            } else if (plan.getProgress() > 0 && "DRAFT".equals(plan.getStatus())) {
                plan.setStatus("IN_PROGRESS");
            }
            planMapper.updateById(plan);
        }
    }

    private CheckinResponse response(Checkin checkin) {
        return responses(List.of(checkin)).getFirst();
    }

    private List<CheckinResponse> responses(List<Checkin> checkins) {
        if (checkins.isEmpty()) return List.of();
        Map<Long, User> users = entities(userMapper.selectBatchIds(checkins.stream()
                .flatMap(checkin -> java.util.stream.Stream.of(checkin.getUserId(), checkin.getAuditedBy()))
                .filter(Objects::nonNull).distinct().toList()), User::getId);
        Map<Long, StudyPlan> plans = entities(selectPlans(checkins), StudyPlan::getId);
        Map<Long, CultureResource> resources = entities(resourceMapper.selectBatchIds(checkins.stream()
                .map(Checkin::getResourceId).distinct().toList()), CultureResource::getId);
        return checkins.stream().map(checkin -> CheckinResponse.from(checkin, users.get(checkin.getUserId()),
                plans.get(checkin.getPlanId()), resources.get(checkin.getResourceId()),
                users.get(checkin.getAuditedBy()))).toList();
    }

    private List<StudyPlan> selectPlans(List<Checkin> checkins) {
        List<Long> ids = checkins.stream().map(Checkin::getPlanId).filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Collections.emptyList() : planMapper.selectBatchIds(ids);
    }

    private <T> Map<Long, T> entities(List<T> values, Function<T, Long> idFunction) {
        return values.stream().collect(Collectors.toMap(idFunction, Function.identity(), (left, right) -> left,
                HashMap::new));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
