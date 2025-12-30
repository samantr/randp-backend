package com.app.dto.person;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PersonCreateRequest(
        @Size(max = 50) String name,
        @Size(max = 50) String lastName,
        @Size(max = 50) String companyName,
        @Size(max = 4000) String address,
        @Size(max = 50) String tel,
        @NotNull Boolean isLegal,
        @Size(max = 4000) String dsc
) {}
