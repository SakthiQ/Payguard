package com.java.PayGuard.expense.dto;

import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency; // Defaults to USD if null

    private String merchantName;

    private String description;

    private String tags; // e.g. "#vacation2026"

    private Boolean taxDeductible;

    private Boolean recurring;

    private Long activityInvoiceId;
}
