package com.wenji.favorite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FavoriteIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired FavoriteMapper favoriteMapper;
    @Autowired CultureCategoryMapper categoryMapper;
    @Autowired CultureResourceMapper resourceMapper;
    @Autowired UserMapper userMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private CultureResource resource;
    private String token;

    @BeforeEach
    void setUp() {
        CultureCategory category = new CultureCategory();
        category.setName("收藏测试分类");
        category.setSortOrder(1);
        category.setStatus(1);
        categoryMapper.insert(category);

        resource = new CultureResource();
        resource.setCategoryId(category.getId());
        resource.setName("收藏测试资源");
        resource.setCity("上海");
        resource.setAddress("测试地址");
        resource.setRecommendedMinutes(90);
        resource.setStatus("PUBLISHED");
        resource.setViewCount(0);
        resource.setFavoriteCount(0);
        resource.setAverageRating(BigDecimal.ZERO);
        resourceMapper.insert(resource);

        User student = new User();
        student.setUsername("favorite_student");
        student.setPasswordHash(passwordEncoder.encode("Student123!"));
        student.setNickname("收藏同学");
        student.setRole("STUDENT");
        student.setPoints(0);
        student.setLevel(1);
        student.setStatus(1);
        userMapper.insert(student);
        token = jwtService.createToken(UserPrincipal.from(student));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/resources/{id}/favorite", resource.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addsFavoriteIdempotentlyAndListsIt() throws Exception {
        favorite(resource.getId());
        favorite(resource.getId());

        assertThat(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getResourceId, resource.getId()))).isEqualTo(1);
        assertThat(resourceMapper.selectById(resource.getId()).getFavoriteCount()).isEqualTo(1);

        mockMvc.perform(get("/api/favorites").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(resource.getId()))
                .andExpect(jsonPath("$.data[0].favorited").value(true));

        mockMvc.perform(get("/api/resources/{id}", resource.getId()).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void removesFavoriteIdempotently() throws Exception {
        favorite(resource.getId());

        mockMvc.perform(delete("/api/resources/{id}/favorite", resource.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/resources/{id}/favorite", resource.getId())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());

        assertThat(favoriteMapper.selectCount(null)).isZero();
        assertThat(resourceMapper.selectById(resource.getId()).getFavoriteCount()).isZero();
    }

    private void favorite(Long resourceId) throws Exception {
        mockMvc.perform(post("/api/resources/{id}/favorite", resourceId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
