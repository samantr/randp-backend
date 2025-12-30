package com.app.dto.item;

public record ItemResponse(
        Long id,
        Long categoryId,
        String categoryTitle,
        String code,
        String title,
        String dsc
) {}
