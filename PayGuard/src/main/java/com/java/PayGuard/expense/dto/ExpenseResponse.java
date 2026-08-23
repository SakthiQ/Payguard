package com.java.PayGuard.expense.dto;

import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private AccountType accountType;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String description;
    private String tags;
    private Boolean taxDeductible;
    private Boolean recurring;
    private Long activityInvoiceId;
    private LocalDateTime createdAt;
}
