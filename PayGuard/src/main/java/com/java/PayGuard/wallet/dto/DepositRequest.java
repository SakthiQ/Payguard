package com.java.PayGuard.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Deposit amount must be greater than zero")
    private BigDecimal amount;
}
