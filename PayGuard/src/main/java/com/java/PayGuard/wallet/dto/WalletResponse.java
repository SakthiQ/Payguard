package com.java.PayGuard.wallet.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private Long walletId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Long userId;
    private String userEmail;
    private LocalDateTime createdAt;
}
