package com.java.PayGuard.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.0100", message = "Transfer amount must be at least 0.01")
    private BigDecimal amount;

    private String description;
}
