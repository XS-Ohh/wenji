package com.wenji.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.common.PageResponse;
import com.wenji.favorite.Favorite;
import com.wenji.favorite.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CultureResourceService {

    private final CultureCategoryMapper categoryMapper;
    private final CultureResourceMapper resourceMapper;
    private final FavoriteMapper favoriteMapper;

    public CultureResourceService(CultureCategoryMapper categoryMapper, CultureResourceMapper resourceMapper,
                                  FavoriteMapper favoriteMapper) {
        this.categoryMapper = categoryMapper;
        this.resourceMapper = resourceMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public List<CategoryResponse> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CultureCategory>()
                        .eq(CultureCategory::getStatus, 1)
                        .orderByAsc(CultureCategory::getSortOrder, CultureCategory::getId))
                .stream().map(CategoryResponse::from).toList();
    }

    public PageResponse<ResourceResponse> list(int page, int pageSize, String keyword, Long categoryId, String city) {
        return list(page, pageSize, keyword, categoryId, city, null);
    }

    public PageResponse<ResourceResponse> list(int page, int pageSize, String keyword, Long categoryId, String city,
                                               Long userId) {
        LambdaQueryWrapper<CultureResource> query = new LambdaQueryWrapper<CultureResource>()
                .eq(CultureResource::getStatus, "PUBLISHED")
                .eq(categoryId != null, CultureResource::getCategoryId, categoryId)
                .eq(StringUtils.hasText(city), CultureResource::getCity, city)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(CultureResource::getName, keyword)
                        .or().like(CultureResource::getSummary, keyword))
                .orderByDesc(CultureResource::getFavoriteCount)
                .orderByAsc(CultureResource::getId);
        Page<CultureResource> result = resourceMapper.selectPage(new Page<>(page, pageSize), query);
        Map<Long, CultureCategory> categories = categoryMap(result.getRecords());
        Set<Long> favoriteIds = favoriteIds(userId, result.getRecords());
        List<ResourceResponse> records = result.getRecords().stream()
                .map(resource -> ResourceResponse.from(resource, categoryName(categories, resource.getCategoryId()),
                        favoriteIds.contains(resource.getId())))
                .toList();
        return PageResponse.from(result, records);
    }

    public List<MapResourceResponse> mapResources(String keyword, Long categoryId, String city) {
        List<CultureResource> resources = resourceMapper.selectList(new LambdaQueryWrapper<CultureResource>()
                .eq(CultureResource::getStatus, "PUBLISHED")
                .eq(categoryId != null, CultureResource::getCategoryId, categoryId)
                .eq(StringUtils.hasText(city), CultureResource::getCity, city)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(CultureResource::getName, keyword)
                        .or().like(CultureResource::getSummary, keyword)
                        .or().like(CultureResource::getAddress, keyword))
                .orderByDesc(CultureResource::getFavoriteCount)
                .orderByAsc(CultureResource::getId));
        Map<Long, CultureCategory> categories = categoryMap(resources);
        return resources.stream()
                .map(resource -> MapResourceResponse.from(resource,
                        categoryName(categories, resource.getCategoryId())))
                .toList();
    }

    @Transactional
    public ResourceResponse detail(Long id) {
        return detail(id, null);
    }

    @Transactional
    public ResourceResponse detail(Long id, Long userId) {
        CultureResource resource = resourceMapper.selectOne(new LambdaQueryWrapper<CultureResource>()
                .eq(CultureResource::getId, id)
                .eq(CultureResource::getStatus, "PUBLISHED"));
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文化资源不存在或未发布");
        }
        resourceMapper.incrementViewCount(id);
        resource.setViewCount(resource.getViewCount() + 1);
        return ResourceResponse.from(resource, requireCategory(resource.getCategoryId()).getName(),
                isFavorited(userId, id));
    }

    public List<ResourceResponse> publishedByIds(List<Long> ids, boolean favorited) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<CultureResource> resources = resourceMapper.selectList(new LambdaQueryWrapper<CultureResource>()
                .in(CultureResource::getId, ids)
                .eq(CultureResource::getStatus, "PUBLISHED"));
        Map<Long, CultureResource> resourcesById = resources.stream()
                .collect(Collectors.toMap(CultureResource::getId, Function.identity()));
        Map<Long, CultureCategory> categories = categoryMap(resources);
        return ids.stream()
                .map(resourcesById::get)
                .filter(Objects::nonNull)
                .map(resource -> ResourceResponse.from(resource,
                        categoryName(categories, resource.getCategoryId()), favorited))
                .toList();
    }

    @Transactional
    public ResourceResponse create(ResourceUpsertRequest request) {
        CultureCategory category = requireCategory(request.categoryId());
        CultureResource resource = new CultureResource();
        copy(request, resource);
        resource.setStatus(StringUtils.hasText(request.status()) ? request.status() : "DRAFT");
        resource.setViewCount(0);
        resource.setFavoriteCount(0);
        resource.setAverageRating(java.math.BigDecimal.ZERO);
        resourceMapper.insert(resource);
        return ResourceResponse.from(resource, category.getName());
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceUpsertRequest request) {
        CultureResource resource = requireAnyResource(id);
        CultureCategory category = requireCategory(request.categoryId());
        copy(request, resource);
        if (StringUtils.hasText(request.status())) {
            resource.setStatus(request.status());
        }
        resourceMapper.updateById(resource);
        return ResourceResponse.from(resourceMapper.selectById(id), category.getName());
    }

    @Transactional
    public void delete(Long id) {
        requireAnyResource(id);
        resourceMapper.deleteById(id);
    }

    @Transactional
    public ResourceResponse changeStatus(Long id, String status) {
        CultureResource resource = requireAnyResource(id);
        resource.setStatus(status);
        resourceMapper.updateById(resource);
        return ResourceResponse.from(resourceMapper.selectById(id), requireCategory(resource.getCategoryId()).getName());
    }

    private Map<Long, CultureCategory> categoryMap(List<CultureResource> resources) {
        if (resources.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = resources.stream().map(CultureResource::getCategoryId).distinct().toList();
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CultureCategory::getId, Function.identity()));
    }

    private String categoryName(Map<Long, CultureCategory> categories, Long id) {
        CultureCategory category = categories.get(id);
        return category == null ? null : category.getName();
    }

    private Set<Long> favoriteIds(Long userId, List<CultureResource> resources) {
        if (userId == null || resources.isEmpty()) {
            return Set.of();
        }
        List<Long> resourceIds = resources.stream().map(CultureResource::getId).toList();
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .in(Favorite::getResourceId, resourceIds))
                .stream().map(Favorite::getResourceId).collect(Collectors.toSet());
    }

    private boolean isFavorited(Long userId, Long resourceId) {
        return userId != null && favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getResourceId, resourceId)) > 0;
    }

    private CultureCategory requireCategory(Long id) {
        CultureCategory category = categoryMapper.selectById(id);
        if (category == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文化分类不存在或已停用");
        }
        return category;
    }

    private CultureResource requireAnyResource(Long id) {
        CultureResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文化资源不存在");
        }
        return resource;
    }

    private void copy(ResourceUpsertRequest request, CultureResource target) {
        target.setCategoryId(request.categoryId());
        target.setName(request.name());
        target.setCity(request.city());
        target.setDistrict(request.district());
        target.setAddress(request.address());
        target.setLongitude(request.longitude());
        target.setLatitude(request.latitude());
        target.setSummary(request.summary());
        target.setDescription(request.description());
        target.setOpeningHours(request.openingHours());
        target.setTicketInfo(request.ticketInfo());
        target.setRecommendedMinutes(request.recommendedMinutes());
        target.setCoverImage(request.coverImage());
    }
}
