package com.app.dto.debt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DebtCreateRequest(
        @NotNull Long projectId,
        @NotNull Long personId,
        @NotNull LocalDate dateDue,
        @NotNull LocalDateTime dateRegistered,
        @Size(max = 4000) String dsc,
        @NotNull @Size(min = 1) List<@Valid DebtLineRequest> lines
) {}
