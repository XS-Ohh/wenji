package com.wenji.plan;

import com.wenji.auth.UserPrincipal;
import com.wenji.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class StudyPlanController {

    private final StudyPlanService service;

    public StudyPlanController(StudyPlanService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<PlanResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody PlanUpsertRequest request) {
        return ApiResponse.success(service.create(principal.id(), request));
    }

    @GetMapping
    public ApiResponse<List<PlanResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.list(principal.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanResponse> detail(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.detail(principal.id(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanResponse> update(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody PlanUpsertRequest request) {
        return ApiResponse.success(service.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(principal.id(), id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PlanResponse> status(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody PlanStatusRequest request) {
        return ApiResponse.success(service.changeStatus(principal.id(), id, request.status()));
    }

    @PostMapping("/{id}/items")
    public ApiResponse<PlanItemResponse> addItem(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody PlanItemUpsertRequest request) {
        return ApiResponse.success(service.addItem(principal.id(), id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<PlanItemResponse> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody PlanItemUpsertRequest request) {
        return ApiResponse.success(service.updateItem(principal.id(), id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        service.deleteItem(principal.id(), id, itemId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/items/reorder")
    public ApiResponse<List<PlanItemResponse>> reorder(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody PlanReorderRequest request) {
        return ApiResponse.success(service.reorder(principal.id(), id, request.itemIds()));
    }
}
