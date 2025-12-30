package com.app.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LedgerRowResponse(
        Long transactionId,
        LocalDate dateRegistered,
        String code,
        Long fromPersonId,
        Long toPersonId,
        BigDecimal amount,
        BigDecimal deltaForPerson,
        BigDecimal runningBalance,
        String dsc
) {}
