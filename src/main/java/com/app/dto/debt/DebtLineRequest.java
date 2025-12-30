package com.app.dto.debt;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DebtLineRequest(
        @NotNull Long itemId,
        @NotNull Long unitId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qnt,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal unitPrice,
        @Size(max = 4000) String dsc
) {}
