package com.wenji.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenji.ai.AiRouteClient;
import com.wenji.ai.AiRouteLog;
import com.wenji.ai.AiRouteLogMapper;
import com.wenji.ai.AiRouteProperties;
import com.wenji.auth.JwtService;
import com.wenji.auth.UserPrincipal;
import com.wenji.resource.CultureCategory;
import com.wenji.resource.CultureCategoryMapper;
import com.wenji.resource.CultureResource;
import com.wenji.resource.CultureResourceMapper;
import com.wenji.user.User;
import com.wenji.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyPlanIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserMapper userMapper;
    @Autowired CultureCategoryMapper categoryMapper;
    @Autowired CultureResourceMapper resourceMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired AiRouteProperties aiRouteProperties;
    @Autowired AiRouteLogMapper aiRouteLogMapper;
    @MockitoBean AiRouteClient aiRouteClient;

    private CultureResource firstResource;
    private CultureResource secondResource;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        CultureCategory category = new CultureCategory();
        category.setName("计划测试分类");
        category.setSortOrder(1);
        category.setStatus(1);
        categoryMapper.insert(category);
        firstResource = resource(category.getId(), "计划资源一");
        secondResource = resource(category.getId(), "计划资源二");
        CultureResource thirdResource = resource(category.getId(), "计划资源三");
        CultureResource fourthResource = resource(category.getId(), "计划资源四");
        firstResource.setFavoriteCount(20);
        secondResource.setFavoriteCount(10);
        thirdResource.setAverageRating(new BigDecimal("4.80"));
        fourthResource.setAverageRating(new BigDecimal("4.60"));
        resourceMapper.updateById(firstResource);
        resourceMapper.updateById(secondResource);
        resourceMapper.updateById(thirdResource);
        resourceMapper.updateById(fourthResource);
        aiRouteProperties.setEnabled(false);
        ownerToken = token(user("plan_owner"));
        otherToken = token(user("plan_other"));
    }

    @Test
    void generatesAndPersistsRuleFallbackRoute() throws Exception {
        long planId = createPlan();

        mockMvc.perform(post("/api/plans/{id}/ai-route", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"desiredPlaces":4,"dailyStartTime":"09:00","dailyEndTime":"17:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.plan.aiGenerated").value(true))
                .andExpect(jsonPath("$.data.plan.itemCount").value(4))
                .andExpect(jsonPath("$.data.plan.items[0].resourceId").value(firstResource.getId()))
                .andExpect(jsonPath("$.data.plan.items[0].startTime").value("09:00:00"));

        long logCount = aiRouteLogMapper.selectCount(new LambdaQueryWrapper<AiRouteLog>()
                .eq(AiRouteLog::getPlanId, planId)
                .eq(AiRouteLog::getStatus, "FALLBACK"));
        org.junit.jupiter.api.Assertions.assertEquals(1, logCount);
    }

    @Test
    void retriesInvalidAiOutputOnceThenFallsBack() throws Exception {
        long planId = createPlan();
        aiRouteProperties.setEnabled(true);
        aiRouteProperties.setBaseUrl("https://example.invalid/v1");
        aiRouteProperties.setApiKey("test-key-not-logged");
        aiRouteProperties.setModel("test-model");
        when(aiRouteClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("not-json");

        mockMvc.perform(post("/api/plans/{id}/ai-route", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredPlaces\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.plan.aiGenerated").value(true));

        verify(aiRouteClient, times(2)).generate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsInvalidDailyTimeWindow() throws Exception {
        long planId = createPlan();

        mockMvc.perform(post("/api/plans/{id}/ai-route", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyStartTime\":\"17:00\",\"dailyEndTime\":\"09:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void rejectsInvalidDateRange() throws Exception {
        mockMvc.perform(post("/api/plans")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody("2026-09-03", "2026-09-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void preventsReadingOrUpdatingAnotherUsersPlan() throws Exception {
        long planId = createPlan();

        mockMvc.perform(get("/api/plans/{id}", planId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/plans/{id}", planId)
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody("2026-09-01", "2026-09-03")))
                .andExpect(status().isNotFound());
    }

    @Test
    void managesStatusAndRouteItems() throws Exception {
        long planId = createPlan();
        long firstItemId = addItem(planId, firstResource.getId(), "09:00", "10:30");
        long secondItemId = addItem(planId, secondResource.getId(), "11:00", "12:30");

        mockMvc.perform(put("/api/plans/{id}/items/reorder", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemIds\":[%d,%d]}".formatted(secondItemId, firstItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secondItemId))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1));

        mockMvc.perform(patch("/api/plans/{id}/status", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progress").value(100));

        mockMvc.perform(delete("/api/plans/{id}/items/{itemId}", planId, firstItemId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/plans/{id}", planId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(1));
    }

    private long createPlan() throws Exception {
        String response = mockMvc.perform(post("/api/plans")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(planBody("2026-09-01", "2026-09-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long addItem(long planId, long resourceId, String startTime, String endTime) throws Exception {
        String response = mockMvc.perform(post("/api/plans/{id}/items", planId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId":%d,"visitDate":"2026-09-02","startTime":"%s","endTime":"%s"}
                                """.formatted(resourceId, startTime, endTime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).path("data");
        return data.path("id").asLong();
    }

    private String planBody(String startDate, String endDate) {
        return """
                {"title":"上海文化研学","city":"上海","startDate":"%s","endDate":"%s",
                 "budget":300,"interests":"博物馆,历史建筑","startLocation":"人民广场"}
                """.formatted(startDate, endDate);
    }

    private CultureResource resource(Long categoryId, String name) {
        CultureResource resource = new CultureResource();
        resource.setCategoryId(categoryId);
        resource.setName(name);
        resource.setCity("上海");
        resource.setAddress("测试地址");
        resource.setRecommendedMinutes(90);
        resource.setStatus("PUBLISHED");
        resource.setViewCount(0);
        resource.setFavoriteCount(0);
        resource.setAverageRating(BigDecimal.ZERO);
        resourceMapper.insert(resource);
        return resource;
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("Student123!"));
        user.setNickname(username);
        user.setRole("STUDENT");
        user.setPoints(0);
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private String token(User user) {
        return jwtService.createToken(UserPrincipal.from(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
