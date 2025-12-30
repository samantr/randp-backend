package com.app.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemUpdateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 50) String title,
        @Size(max = 4000) String dsc
) {}
