package com.java.PayGuard.expense.dto;

import com.java.PayGuard.expense.entity.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetProgressDto {
    private Long id;
    private ExpenseCategory category;
    private BigDecimal monthlyCap;
    private BigDecimal spentAmount;
    private BigDecimal rolloverAmount;
    private BigDecimal remainingAmount;
    private Double percentUsed;
    private String statusPill; // OK, WARNING_80, OVERSPENT_100
}
