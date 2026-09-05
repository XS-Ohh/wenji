package com.wenji.checkin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckinCreateRequest(
        Long planId,
        @NotNull Long resourceId,
        @Size(max = 255) String imageUrl,
        @Size(max = 500) String content) {
}
