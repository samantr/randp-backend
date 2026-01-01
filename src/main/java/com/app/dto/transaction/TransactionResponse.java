package com.app.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long projectId,
        Long fromPersonId,
        Long toPersonId,
        String code,
        LocalDate dateDue,
        BigDecimal amountPaid,
        String paymentType,
        String transactionType,
        LocalDateTime dateRegistered,
        String dsc,

        BigDecimal allocatedAmount,
        BigDecimal remainingAmount
) {}
