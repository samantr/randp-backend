package com.app.dto.transactiontrack;

import java.math.BigDecimal;

public record AllocationResponse(
        Long id,
        Long debtHeaderId,
        Long transactionId,
        BigDecimal coveredAmount,
        String dsc
) {}
