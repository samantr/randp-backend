package com.app.dto.debt;

import java.time.LocalDate;

public record DebtHeaderResponse(
        Long id,
        Long projectId,
        Long personId,
        LocalDate dateDue,
        LocalDate dateRegistered,
        String dsc
) {}
