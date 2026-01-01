package com.app.dto.transactiontrack;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionCandidateResponse(
        Long id,
        String code,
        LocalDateTime dateRegistered,
        BigDecimal amountPaid,
        BigDecimal allocatedAmount,
        BigDecimal remainingAmount,
        BigDecimal editableRemainingAmount
) {}
