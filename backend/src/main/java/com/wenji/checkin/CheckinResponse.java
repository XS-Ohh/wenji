package com.wenji.checkin;

import com.wenji.plan.StudyPlan;
import com.wenji.resource.CultureResource;
import com.wenji.user.User;

import java.time.LocalDateTime;

public record CheckinResponse(
        Long id,
        Long userId,
        String userNickname,
        Long planId,
        String planTitle,
        Long resourceId,
        String resourceName,
        String resourceAddress,
        LocalDateTime checkinTime,
        String imageUrl,
        String content,
        String status,
        String auditComment,
        Long auditedBy,
        String auditorNickname,
        LocalDateTime auditedAt,
        LocalDateTime createdAt) {

    public static CheckinResponse from(Checkin checkin, User user, StudyPlan plan,
                                       CultureResource resource, User auditor) {
        return new CheckinResponse(checkin.getId(), checkin.getUserId(),
                user == null ? null : user.getNickname(), checkin.getPlanId(),
                plan == null ? null : plan.getTitle(), checkin.getResourceId(),
                resource == null ? null : resource.getName(), resource == null ? null : resource.getAddress(),
                checkin.getCheckinTime(), checkin.getImageUrl(), checkin.getContent(), checkin.getStatus(),
                checkin.getAuditComment(), checkin.getAuditedBy(),
                auditor == null ? null : auditor.getNickname(), checkin.getAuditedAt(), checkin.getCreatedAt());
    }
}
