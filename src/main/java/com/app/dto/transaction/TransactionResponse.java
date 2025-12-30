package com.app.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        LocalDate dateRegistered,
        String dsc
) {}
