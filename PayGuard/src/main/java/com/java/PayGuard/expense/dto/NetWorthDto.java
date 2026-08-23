package com.java.PayGuard.expense.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetWorthDto {
    private BigDecimal totalAssets;       // Wallet Balance + Savings Goals current balance
    private BigDecimal totalLiabilities;  // Credit Account Expenses + Unpaid Invoices
    private BigDecimal netWorth;         // totalAssets - totalLiabilities
    private String currency;
}
