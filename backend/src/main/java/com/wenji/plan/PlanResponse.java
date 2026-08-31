package com.wenji.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlanResponse(
        Long id,
        String title,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String interests,
        String startLocation,
        String status,
        int progress,
        boolean aiGenerated,
        int itemCount,
        List<PlanItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PlanResponse from(StudyPlan plan, List<PlanItemResponse> items) {
        return new PlanResponse(plan.getId(), plan.getTitle(), plan.getCity(), plan.getStartDate(), plan.getEndDate(),
                plan.getBudget(), plan.getInterests(), plan.getStartLocation(), plan.getStatus(), plan.getProgress(),
                plan.getAiGenerated() != null && plan.getAiGenerated() == 1, items.size(), items,
                plan.getCreatedAt(), plan.getUpdatedAt());
    }
}
