package com.app.dto.debt;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DebtHeaderResponse(
        Long id,
        Long projectId,
        Long personId,
        LocalDate dateDue,
        LocalDateTime dateRegistered,
        String dsc
) {}
