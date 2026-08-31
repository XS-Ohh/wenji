package com.wenji.favorite;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import com.wenji.resource.ResourceResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @PostMapping("/resources/{id}/favorite")
    public ApiResponse<Void> add(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        service.add(principal.id(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/resources/{id}/favorite")
    public ApiResponse<Void> remove(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        service.remove(principal.id(), id);
        return ApiResponse.success();
    }

    @GetMapping("/favorites")
    public ApiResponse<List<ResourceResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.list(principal.id()));
    }
}
