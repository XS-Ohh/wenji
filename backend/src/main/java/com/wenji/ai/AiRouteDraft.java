package com.wenji.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRouteDraft(
        String title,
        String summary,
        BigDecimal estimatedBudget,
        List<Day> days,
        List<String> tips) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Day(LocalDate date, List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long resourceId,
            LocalTime startTime,
            LocalTime endTime,
            String transportation,
            String reason) {
    }
}
