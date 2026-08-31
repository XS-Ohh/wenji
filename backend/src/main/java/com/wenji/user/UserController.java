package com.wenji.user;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getCurrent(principal.id()));
    }

    @PutMapping
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateCurrent(principal.id(), request));
    }

    @GetMapping("/summary")
    public ApiResponse<UserSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.summary(principal.id()));
    }
}

