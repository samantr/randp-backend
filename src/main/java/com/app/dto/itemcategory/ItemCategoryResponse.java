package com.app.dto.itemcategory;

public record ItemCategoryResponse(
        Long id,
        String title,
        Long parentId,
        String dsc
) {}
