package com.app.dto.debt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record DebtUpdateRequest(
        @NotNull Long projectId,
        @NotNull Long personId,
        @NotNull LocalDate dateDue,
        @NotNull LocalDate dateRegistered,
        @Size(max = 4000) String dsc,
        @NotNull @Size(min = 1) List<@Valid DebtLineRequest> lines
) {}
