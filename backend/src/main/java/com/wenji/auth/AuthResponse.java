package com.wenji.auth;

import com.wenji.user.UserResponse;

public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    @Override
    public String toString() {
        return "AuthResponse[token=[REDACTED], tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }
}
