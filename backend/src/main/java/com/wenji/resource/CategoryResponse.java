package com.wenji.resource;

public record CategoryResponse(Long id, String name, String icon, String defaultCover, int sortOrder) {

    public static CategoryResponse from(CultureCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getIcon(),
                category.getDefaultCover(), category.getSortOrder());
    }
}

