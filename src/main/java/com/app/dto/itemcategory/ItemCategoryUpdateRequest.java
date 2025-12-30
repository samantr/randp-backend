package com.app.dto.itemcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemCategoryUpdateRequest(
        @NotBlank @Size(max = 50) String title,
        Long parentId,
        @Size(max = 4000) String dsc
) {}
