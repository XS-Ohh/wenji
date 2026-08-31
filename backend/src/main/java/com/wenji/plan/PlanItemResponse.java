package com.wenji.plan;

import com.wenji.resource.CultureResource;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlanItemResponse(
        Long id,
        Long resourceId,
        String resourceName,
        String resourceAddress,
        String coverImage,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        int sortOrder,
        String transportation,
        String reason,
        String status) {

    public static PlanItemResponse from(PlanItem item, CultureResource resource) {
        return new PlanItemResponse(item.getId(), item.getResourceId(), resource.getName(), resource.getAddress(),
                resource.getCoverImage(), item.getVisitDate(), item.getStartTime(), item.getEndTime(),
                item.getSortOrder(), item.getTransportation(), item.getReason(), item.getStatus());
    }
}
