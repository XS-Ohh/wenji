package com.wenji.user;

import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getCurrent(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateCurrent(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setNickname(request.nickname());
        user.setAvatarUrl(request.avatarUrl());
        userMapper.updateById(user);
        return UserResponse.from(userMapper.selectById(userId));
    }

    public UserSummaryResponse summary(Long userId) {
        User user = requireUser(userId);
        // Later P0 modules replace the zero placeholders with aggregate queries.
        return new UserSummaryResponse(user.getPoints(), user.getLevel(), 0, 0, 0, 0);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}

