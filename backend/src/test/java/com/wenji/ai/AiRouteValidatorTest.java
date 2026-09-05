package com.wenji.ai;

import com.wenji.plan.StudyPlan;
import com.wenji.resource.CultureResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRouteValidatorTest {

    private final AiRouteValidator validator = new AiRouteValidator();

    @Test
    void rejectsResourceOutsideCandidateWhitelist() {
        AiRouteDraft draft = route(List.of(item(999L, "09:00", "10:00")));

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(draft, plan(), Map.of(1L, resource(1L)), request()));
    }

    @Test
    void rejectsOverlappingItemsOnTheSameDay() {
        AiRouteDraft draft = route(List.of(item(1L, "09:00", "11:00"), item(2L, "10:30", "12:00")));

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(draft, plan(), Map.of(1L, resource(1L), 2L, resource(2L)), request()));
    }

    private AiRouteDraft route(List<AiRouteDraft.Item> items) {
        return new AiRouteDraft("测试路线", "测试摘要", BigDecimal.TEN,
                List.of(new AiRouteDraft.Day(LocalDate.of(2026, 9, 1), items)), List.of());
    }

    private AiRouteDraft.Item item(Long resourceId, String start, String end) {
        return new AiRouteDraft.Item(resourceId, LocalTime.parse(start), LocalTime.parse(end), "步行", "测试");
    }

    private StudyPlan plan() {
        StudyPlan plan = new StudyPlan();
        plan.setStartDate(LocalDate.of(2026, 9, 1));
        plan.setEndDate(LocalDate.of(2026, 9, 2));
        return plan;
    }

    private CultureResource resource(Long id) {
        CultureResource resource = new CultureResource();
        resource.setId(id);
        resource.setStatus("PUBLISHED");
        return resource;
    }

    private AiRouteRequest request() {
        return new AiRouteRequest(3, LocalTime.of(9, 0), LocalTime.of(17, 0));
    }
}
