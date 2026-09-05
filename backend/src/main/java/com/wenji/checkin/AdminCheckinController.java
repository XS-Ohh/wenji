package com.wenji.checkin;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/checkins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCheckinController {

    private final CheckinService service;

    public AdminCheckinController(CheckinService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CheckinResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.listForReview(status));
    }

    @PatchMapping("/{id}/review")
    public ApiResponse<CheckinResponse> review(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody CheckinReviewRequest request) {
        return ApiResponse.success(service.review(principal.id(), id, request));
    }
}
