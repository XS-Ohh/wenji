package com.wenji.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 50) String nickname,
        @Size(max = 255) String avatarUrl) {
}

