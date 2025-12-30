package com.app.dto.debt;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DebtAllocationView(
        Long allocationId,
        Long transactionId,
        String transactionCode,
        LocalDate transactionDateRegistered,
        BigDecimal transactionAmountPaid,
        BigDecimal coveredAmount,
        String dsc
) {}
