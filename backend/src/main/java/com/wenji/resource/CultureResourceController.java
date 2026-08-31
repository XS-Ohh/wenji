package com.wenji.resource;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import com.wenji.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class CultureResourceController {

    private final CultureResourceService service;

    public CultureResourceController(CultureResourceService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.success(service.categories());
    }

    @GetMapping("/resources")
    public ApiResponse<PageResponse<ResourceResponse>> resources(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.list(page, pageSize, keyword, categoryId, city,
                principal == null ? null : principal.id()));
    }

    @GetMapping("/resources/{id}")
    public ApiResponse<ResourceResponse> detail(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.detail(id, principal == null ? null : principal.id()));
    }
}
