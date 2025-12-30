package com.app.dto.transaction;

import java.math.BigDecimal;

public record PersonBalanceResponse(
        Long projectId,
        Long personId,
        BigDecimal totalIn,
        BigDecimal totalOut,
        BigDecimal net
) {}
