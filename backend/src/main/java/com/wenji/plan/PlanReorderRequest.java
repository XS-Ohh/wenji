package com.wenji.plan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PlanReorderRequest(@NotEmpty List<@NotNull Long> itemIds) {
}
