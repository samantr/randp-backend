package com.app.dto.itemcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemCategoryCreateRequest(
        @NotBlank @Size(max = 50) String title,
        Long parentId,
        @Size(max = 4000) String dsc
) {}
