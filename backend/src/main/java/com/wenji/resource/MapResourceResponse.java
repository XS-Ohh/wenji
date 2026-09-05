package com.wenji.resource;

import java.math.BigDecimal;

public record MapResourceResponse(
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
        String openingHours,
        String coverImage,
        int favoriteCount,
        BigDecimal averageRating) {

    public static MapResourceResponse from(CultureResource resource, String categoryName) {
        return new MapResourceResponse(resource.getId(), resource.getCategoryId(), categoryName,
                resource.getName(), resource.getCity(), resource.getDistrict(), resource.getAddress(),
                resource.getLongitude(), resource.getLatitude(), resource.getSummary(),
                resource.getOpeningHours(), resource.getCoverImage(), resource.getFavoriteCount(),
                resource.getAverageRating());
    }
}
