package com.wenji.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlanStatusRequest(
        @NotBlank
        @Pattern(regexp = "DRAFT|IN_PROGRESS|COMPLETED|CANCELLED")
        String status) {
}
