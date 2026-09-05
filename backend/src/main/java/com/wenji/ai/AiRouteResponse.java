package com.wenji.ai;

import com.wenji.plan.PlanResponse;

import java.math.BigDecimal;
import java.util.List;

public record AiRouteResponse(
        String title,
        String summary,
        BigDecimal estimatedBudget,
        List<AiRouteDraft.Day> days,
        List<String> tips,
        boolean fallback,
        PlanResponse plan) {

    public static AiRouteResponse from(AiRouteDraft draft, boolean fallback, PlanResponse plan) {
        return new AiRouteResponse(draft.title(), draft.summary(), draft.estimatedBudget(), draft.days(),
                draft.tips(), fallback, plan);
    }
}
