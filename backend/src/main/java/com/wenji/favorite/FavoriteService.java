package com.wenji.favorite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wenji.common.BusinessException;
import com.wenji.common.ErrorCode;
import com.wenji.resource.CultureResource;
import com.wenji.resource.CultureResourceMapper;
import com.wenji.resource.CultureResourceService;
import com.wenji.resource.ResourceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final CultureResourceMapper resourceMapper;
    private final CultureResourceService resourceService;

    public FavoriteService(FavoriteMapper favoriteMapper, CultureResourceMapper resourceMapper,
                           CultureResourceService resourceService) {
        this.favoriteMapper = favoriteMapper;
        this.resourceMapper = resourceMapper;
        this.resourceService = resourceService;
    }

    @Transactional
    public void add(Long userId, Long resourceId) {
        requirePublishedResource(resourceId);
        if (favoriteMapper.selectCount(query(userId, resourceId)) > 0) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setResourceId(resourceId);
        favoriteMapper.insert(favorite);
        resourceMapper.incrementFavoriteCount(resourceId);
    }

    @Transactional
    public void remove(Long userId, Long resourceId) {
        int deleted = favoriteMapper.delete(query(userId, resourceId));
        if (deleted > 0) {
            resourceMapper.decrementFavoriteCount(resourceId);
        }
    }

    public List<ResourceResponse> list(Long userId) {
        List<Long> resourceIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt)
                        .orderByDesc(Favorite::getId))
                .stream().map(Favorite::getResourceId).toList();
        return resourceService.publishedByIds(resourceIds, true);
    }

    private LambdaQueryWrapper<Favorite> query(Long userId, Long resourceId) {
        return new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getResourceId, resourceId);
    }

    private void requirePublishedResource(Long resourceId) {
        CultureResource resource = resourceMapper.selectById(resourceId);
        if (resource == null || !"PUBLISHED".equals(resource.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文化资源不存在或未发布");
        }
    }
}
