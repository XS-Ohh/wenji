package com.wenji.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenji.auth.JwtService;
import com.wenji.auth.UserPrincipal;
import com.wenji.plan.PlanItem;
import com.wenji.plan.PlanItemMapper;
import com.wenji.plan.StudyPlan;
import com.wenji.plan.StudyPlanMapper;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckinIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired UserMapper userMapper;
    @Autowired StudyPlanMapper planMapper;
    @Autowired PlanItemMapper itemMapper;
    @Autowired CultureCategoryMapper categoryMapper;
    @Autowired CultureResourceMapper resourceMapper;
    @Autowired CheckinMapper checkinMapper;

    private User student;
    private String studentToken;
    private String adminToken;
    private StudyPlan plan;
    private CultureResource firstResource;
    private CultureResource secondResource;

    @BeforeEach
    void setUp() {
        student = user("checkin_student", "STUDENT");
        User admin = user("checkin_admin", "ADMIN");
        studentToken = token(student);
        adminToken = token(admin);

        CultureCategory category = new CultureCategory();
        category.setName("打卡测试分类");
        category.setSortOrder(1);
        category.setStatus(1);
        categoryMapper.insert(category);
        firstResource = resource(category.getId(), "打卡地点一");
        secondResource = resource(category.getId(), "打卡地点二");

        plan = new StudyPlan();
        plan.setUserId(student.getId());
        plan.setTitle("打卡测试计划");
        plan.setCity("上海");
        plan.setStartDate(LocalDate.of(2026, 9, 1));
        plan.setEndDate(LocalDate.of(2026, 9, 2));
        plan.setStatus("DRAFT");
        plan.setProgress(0);
        plan.setAiGenerated(0);
        planMapper.insert(plan);
        item(plan.getId(), firstResource.getId(), 1);
        item(plan.getId(), secondResource.getId(), 2);
    }

    @Test
    void submitsAndListsCheckinWhilePreventingDuplicate() throws Exception {
        long checkinId = submit(firstResource.getId(), plan.getId(), "第一次文化观察");

        mockMvc.perform(get("/api/checkins").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(checkinId))
                .andExpect(jsonPath("$.data[0].resourceName").value("打卡地点一"))
                .andExpect(jsonPath("$.data[0].planTitle").value("打卡测试计划"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(post("/api/checkins")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkinBody(firstResource.getId(), plan.getId(), "重复打卡")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void adminCannotUseStudentCheckinEndpoints() throws Exception {
        mockMvc.perform(get("/api/checkins").header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/checkins")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkinBody(firstResource.getId(), null, "管理员不应发起打卡")))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminReviewsAndApprovalIsIdempotent() throws Exception {
        student.setPoints(95);
        userMapper.updateById(student);
        long checkinId = submit(firstResource.getId(), plan.getId(), "等待审核");

        mockMvc.perform(patch("/api/admin/checkins/{id}/review", checkinId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        approve(checkinId);
        approve(checkinId);

        assertThat(userMapper.selectById(student.getId()).getPoints()).isEqualTo(105);
        assertThat(userMapper.selectById(student.getId()).getLevel()).isEqualTo(2);
        assertThat(planMapper.selectById(plan.getId()).getProgress()).isEqualTo(50);
        assertThat(planMapper.selectById(plan.getId()).getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(itemMapper.selectList(null).stream()
                .filter(item -> item.getResourceId().equals(firstResource.getId()))
                .findFirst().orElseThrow().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void rejectedCheckinCanBeCorrectedAndResubmitted() throws Exception {
        long checkinId = submit(secondResource.getId(), null, "需要修改的内容");

        mockMvc.perform(patch("/api/admin/checkins/{id}/review", checkinId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"auditComment\":\"请补充观察细节\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(post("/api/checkins")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkinBody(secondResource.getId(), null, "已经补充观察细节")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(checkinId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.auditComment").doesNotExist());

        assertThat(checkinMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void validatesAndServesUploadedImage() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not-an-image".getBytes());
        mockMvc.perform(multipart("/api/uploads/images")
                        .file(invalid)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isBadRequest());

        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile image = new MockMultipartFile("file", "checkin.png", "image/png", pngHeader);
        String response = mockMvc.perform(multipart("/api/uploads/images")
                        .file(image)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(response).path("data").path("url").asText();

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("image/png"));
    }

    private long submit(Long resourceId, Long planId, String content) throws Exception {
        String response = mockMvc.perform(post("/api/checkins")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkinBody(resourceId, planId, content)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void approve(long checkinId) throws Exception {
        mockMvc.perform(patch("/api/admin/checkins/{id}/review", checkinId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"auditComment\":\"内容真实完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    private String checkinBody(Long resourceId, Long planId, String content) {
        return "{\"resourceId\":%d,%s\"content\":\"%s\"}".formatted(resourceId,
                planId == null ? "" : "\"planId\":" + planId + ",", content);
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("Student123!"));
        user.setNickname(username);
        user.setRole(role);
        user.setPoints(0);
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
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

    private void item(Long planId, Long resourceId, int order) {
        PlanItem item = new PlanItem();
        item.setPlanId(planId);
        item.setResourceId(resourceId);
        item.setSortOrder(order);
        item.setStatus("PENDING");
        itemMapper.insert(item);
    }

    private String token(User user) {
        return jwtService.createToken(UserPrincipal.from(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
