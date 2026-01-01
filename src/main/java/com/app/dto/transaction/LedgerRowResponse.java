package com.app.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LedgerRowResponse(
        Long transactionId,
        LocalDateTime dateRegistered,
        String code,
        Long fromPersonId,
        Long toPersonId,
        BigDecimal amount,
        BigDecimal deltaForPerson,
        BigDecimal runningBalance,
        String dsc
) {}
