package com.wenji.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalTime;

public record AiRouteRequest(
        @Min(3) @Max(4) Integer desiredPlaces,
        LocalTime dailyStartTime,
        LocalTime dailyEndTime) {

    public int resolvedDesiredPlaces() {
        return desiredPlaces == null ? 4 : desiredPlaces;
    }

    public LocalTime resolvedStartTime() {
        return dailyStartTime == null ? LocalTime.of(9, 0) : dailyStartTime;
    }

    public LocalTime resolvedEndTime() {
        return dailyEndTime == null ? LocalTime.of(17, 0) : dailyEndTime;
    }
}
