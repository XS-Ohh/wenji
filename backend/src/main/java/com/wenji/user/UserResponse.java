package com.wenji.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String role,
        int points,
        int level,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(),
                user.getRole(), user.getPoints(), user.getLevel(), user.getCreatedAt());
    }
}

