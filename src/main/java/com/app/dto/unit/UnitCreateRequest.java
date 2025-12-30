package com.app.dto.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnitCreateRequest(
        @NotBlank @Size(max = 50) String title,
        @Size(max = 5000) String dsc
) {}
