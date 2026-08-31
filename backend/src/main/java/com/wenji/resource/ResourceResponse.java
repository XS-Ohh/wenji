package com.wenji.resource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResourceResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String city,
        String district,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String summary,
        String description,
        String openingHours,
        String ticketInfo,
        int recommendedMinutes,
        String coverImage,
        String status,
        int viewCount,
        int favoriteCount,
        boolean favorited,
        BigDecimal averageRating,
        LocalDateTime createdAt) {

    public static ResourceResponse from(CultureResource resource, String categoryName) {
        return from(resource, categoryName, false);
    }

    public static ResourceResponse from(CultureResource resource, String categoryName, boolean favorited) {
        return new ResourceResponse(resource.getId(), resource.getCategoryId(), categoryName, resource.getName(),
                resource.getCity(), resource.getDistrict(), resource.getAddress(), resource.getLongitude(),
                resource.getLatitude(), resource.getSummary(), resource.getDescription(), resource.getOpeningHours(),
                resource.getTicketInfo(), resource.getRecommendedMinutes(), resource.getCoverImage(), resource.getStatus(),
                resource.getViewCount(), resource.getFavoriteCount(), favorited, resource.getAverageRating(), resource.getCreatedAt());
    }
}
