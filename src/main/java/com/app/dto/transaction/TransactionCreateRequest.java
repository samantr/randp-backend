package com.app.dto.transaction;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionCreateRequest(
        @NotNull Long projectId,
        @NotNull Long fromPersonId,
        @NotNull Long toPersonId,

        @NotBlank @Size(max = 50) String code,
        @NotNull LocalDate dateDue,

        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amountPaid,

        @NotBlank @Size(min = 3, max = 3) String paymentType,
        @NotBlank @Size(min = 3, max = 3) String transactionType,

        @NotNull LocalDateTime dateRegistered,
        @Size(max = 4000) String dsc
) {}
