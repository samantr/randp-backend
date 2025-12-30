package com.app.dto.debt;

import java.math.BigDecimal;

public record DebtLineResponse(
        Long id,
        Long itemId,
        String itemTitle,
        Long unitId,
        String unitTitle,
        BigDecimal qnt,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String dsc
) {}
