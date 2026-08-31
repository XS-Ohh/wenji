package com.wenji.resource;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ResourceUpsertRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String city,
        @Size(max = 50) String district,
        @NotBlank @Size(max = 255) String address,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @Size(max = 500) String summary,
        String description,
        @Size(max = 255) String openingHours,
        @Size(max = 255) String ticketInfo,
        @NotNull @Positive Integer recommendedMinutes,
        @Size(max = 255) String coverImage,
        @jakarta.validation.constraints.Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE") String status) {
}

