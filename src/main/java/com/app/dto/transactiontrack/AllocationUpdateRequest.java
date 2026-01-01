package com.app.dto.transactiontrack;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AllocationUpdateRequest(
        @NotNull Long transactionId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal coveredAmount,
        @Size(max = 5000) String dsc
) {}
