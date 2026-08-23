package com.java.PayGuard.wallet.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositResponse {
    private Long walletId;
    private String accountNumber;
    private BigDecimal newBalance;
    private String transactionReference;
    private String message;
}
