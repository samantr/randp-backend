package com.app.dto.debt;

import java.math.BigDecimal;
import java.util.List;

public record DebtViewResponse(
        DebtHeaderResponse header,
        List<DebtLineResponse> lines,
        List<DebtAllocationView> allocations,
        BigDecimal totalAmount,
        BigDecimal coveredAmount,
        BigDecimal remainingAmount
) {}
