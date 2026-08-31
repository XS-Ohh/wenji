package com.wenji.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResourceStatusRequest(
        @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE", message = "必须为DRAFT、PUBLISHED或OFFLINE")
        String status) {
}

