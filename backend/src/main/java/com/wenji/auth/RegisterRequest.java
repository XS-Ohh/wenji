package com.wenji.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$", message = "仅支持3-50位字母、数字或下划线")
        String username,
        @NotBlank @Size(min = 8, max = 72, message = "长度须为8-72位")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "必须包含大写字母、小写字母、数字和特殊字符")
        String password,
        @NotBlank @Size(max = 50)
        String nickname) {

    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=[REDACTED], nickname=" + nickname + "]";
    }
}
