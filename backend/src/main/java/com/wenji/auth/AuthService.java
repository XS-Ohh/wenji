package com.wenji.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.user.User;
import com.wenji.user.UserMapper;
import com.wenji.user.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       @Value("${wenji.jwt.expiration-seconds}") long expirationSeconds) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole("STUDENT");
        user.setPoints(0);
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);

        UserPrincipal principal = UserPrincipal.from(user);
        return response(principal, user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            UserPrincipal principal = (UserPrincipal) authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())).getPrincipal();
            User user = userMapper.selectById(principal.id());
            return response(principal, user);
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
    }

    private AuthResponse response(UserPrincipal principal, User user) {
        return new AuthResponse(jwtService.createToken(principal), "Bearer", expirationSeconds, UserResponse.from(user));
    }
}

