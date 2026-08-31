package com.wenji.resource;

import com.wenji.auth.JwtService;
import com.wenji.auth.UserPrincipal;
import com.wenji.user.User;
import com.wenji.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CultureResourceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CultureCategoryMapper categoryMapper;
    @Autowired CultureResourceMapper resourceMapper;
    @Autowired UserMapper userMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private CultureCategory museum;
    private CultureResource published;
    private User admin;

    @BeforeEach
    void setUp() {
        museum = new CultureCategory();
        museum.setName("博物馆");
        museum.setSortOrder(1);
        museum.setStatus(1);
        categoryMapper.insert(museum);

        published = resource("上海博物馆", "上海", "PUBLISHED");
        resource("内部草稿", "上海", "DRAFT");
        resource("苏州博物馆", "苏州", "PUBLISHED");
        admin = user("admin", "ADMIN");
    }

    @Test
    void filtersPublishedResourcesByKeywordCategoryAndCity() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .param("keyword", "上海")
                        .param("categoryId", museum.getId().toString())
                        .param("city", "上海")
                        .param("page", "1")
                        .param("pageSize", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("上海博物馆"))
                .andExpect(jsonPath("$.data.records[0].categoryName").value("博物馆"));
    }

    @Test
    void hidesDraftDetailAndAtomicallyCountsPublishedView() throws Exception {
        mockMvc.perform(get("/api/resources/{id}", published.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
        assertThat(resourceMapper.selectById(published.getId()).getViewCount()).isEqualTo(1);

        CultureResource draft = resourceMapper.selectList(null).stream()
                .filter(resource -> resource.getStatus().equals("DRAFT")).findFirst().orElseThrow();
        mockMvc.perform(get("/api/resources/{id}", draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void allowsAdminToCreateResource() throws Exception {
        String token = jwtService.createToken(UserPrincipal.from(admin));
        mockMvc.perform(post("/api/admin/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "name": "城市文化新空间",
                                  "city": "上海",
                                  "district": "徐汇区",
                                  "address": "测试路1号",
                                  "recommendedMinutes": 90,
                                  "status": "PUBLISHED"
                                }
                                """.formatted(museum.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("城市文化新空间"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    private CultureResource resource(String name, String city, String status) {
        CultureResource resource = new CultureResource();
        resource.setCategoryId(museum.getId());
        resource.setName(name);
        resource.setCity(city);
        resource.setAddress("测试地址");
        resource.setSummary(name + "简介");
        resource.setRecommendedMinutes(90);
        resource.setStatus(status);
        resource.setViewCount(0);
        resource.setFavoriteCount(0);
        resource.setAverageRating(BigDecimal.ZERO);
        resourceMapper.insert(resource);
        return resource;
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("Admin123!"));
        user.setNickname(username);
        user.setRole(role);
        user.setPoints(0);
        user.setLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }
}

