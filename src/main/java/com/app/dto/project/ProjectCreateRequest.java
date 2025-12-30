package com.app.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        Long parentId,
        @NotBlank @Size(max = 50) String title,
        @Size(max = 4000) String dsc
) {}
