package com.wenji.plan;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlanItemUpsertRequest(
        @NotNull Long resourceId,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        @Min(1) Integer sortOrder,
        @Size(max = 50) String transportation,
        @Size(max = 255) String reason) {
}
