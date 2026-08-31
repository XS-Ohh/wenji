package com.wenji.plan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanUpsertRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 50) String city,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @DecimalMin("0") BigDecimal budget,
        @Size(max = 255) String interests,
        @Size(max = 255) String startLocation) {
}
