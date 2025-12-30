package com.app.dto.transaction;

import java.math.BigDecimal;

public record PairBalanceResponse(
        Long projectId,
        Long fromPersonId,
        Long toPersonId,
        BigDecimal fromToToTotal,
        BigDecimal toToFromTotal,
        BigDecimal netFromToTo
) {}
