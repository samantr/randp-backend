package com.app.dto.project;

public record ProjectResponse(
        Long id,
        Long parentId,
        String title,
        String dsc
) {}
