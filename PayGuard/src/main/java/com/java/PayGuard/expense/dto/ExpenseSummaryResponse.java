package com.java.PayGuard.expense.dto;

import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSummaryResponse {
    private BigDecimal totalExpenses;
    private BigDecimal totalTaxDeductible;
    private Map<ExpenseCategory, BigDecimal> categoryBreakdown;
    private Map<AccountType, BigDecimal> accountTypeBreakdown;
}
