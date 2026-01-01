package com.app.dto.transactiontrack;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DebtCandidateResponse(
        Long id,
        String personTitle,
        LocalDateTime dateRegistered,
        BigDecimal totalAmount,
        BigDecimal allocatedAmount,
        BigDecimal remainingAmount,
        BigDecimal editableRemainingAmount
) {}
