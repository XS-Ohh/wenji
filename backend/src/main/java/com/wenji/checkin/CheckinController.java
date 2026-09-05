package com.wenji.checkin;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
@PreAuthorize("hasRole('STUDENT')")
public class CheckinController {

    private final CheckinService service;

    public CheckinController(CheckinService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CheckinResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody CheckinCreateRequest request) {
        return ApiResponse.success(service.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<CheckinResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.listMine(principal.id()));
    }
}
