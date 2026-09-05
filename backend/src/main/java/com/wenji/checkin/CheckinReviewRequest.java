package com.wenji.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckinReviewRequest(
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status,
        @Size(max = 255) String auditComment) {
}
