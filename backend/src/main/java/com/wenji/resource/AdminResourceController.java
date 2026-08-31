package com.wenji.resource;

import com.wenji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resources")
@PreAuthorize("hasRole('ADMIN')")
public class AdminResourceController {

    private final CultureResourceService service;

    public AdminResourceController(CultureResourceService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<ResourceResponse> create(@Valid @RequestBody ResourceUpsertRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ResourceUpsertRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ResourceResponse> status(@PathVariable Long id,
                                                @Valid @RequestBody ResourceStatusRequest request) {
        return ApiResponse.success(service.changeStatus(id, request.status()));
    }
}

