package com.java.PayGuard.expense.service;

import com.java.PayGuard.expense.dto.ExpenseSummaryResponse;
import com.java.PayGuard.expense.dto.SubscriptionDto;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseAiInsightService {

    private final ExpenseService expenseService;
    private final SubscriptionService subscriptionService;

    public List<String> generateSpendingInsights(User currentUser) {
        List<String> insights = new ArrayList<>();

        ExpenseSummaryResponse summary = expenseService.getSummary(currentUser);
        List<SubscriptionDto> subs = subscriptionService.getSubscriptions(currentUser);

        BigDecimal totalSpent = summary.getTotalExpenses();

        // Insight 1: Category Dominance
        if (summary.getCategoryBreakdown() != null && !summary.getCategoryBreakdown().isEmpty()) {
            Map.Entry<ExpenseCategory, BigDecimal> topCat = summary.getCategoryBreakdown().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topCat != null && totalSpent.compareTo(BigDecimal.ZERO) > 0) {
                double pct = topCat.getValue().multiply(new BigDecimal("100"))
                        .divide(totalSpent, 1, java.math.RoundingMode.HALF_EVEN)
                        .doubleValue();
                insights.add("💡 Spending Pattern: " + topCat.getKey() + " accounts for " + pct + "% ($" + topCat.getValue() + ") of your total expenses.");
            }
        }

        // Insight 2: Subscriptions Optimization Tip
        if (subs != null && !subs.isEmpty()) {
            BigDecimal subTotal = subs.stream()
                    .map(SubscriptionDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            insights.add("📊 Subscription Audit: You have " + subs.size() + " active subscriptions costing $" + subTotal + "/month. Review unused services to save money.");
        }

        // Insight 3: Tax Deductibles
        if (summary.getTotalTaxDeductible() != null && summary.getTotalTaxDeductible().compareTo(BigDecimal.ZERO) > 0) {
            insights.add("🧾 Tax Alert: You have $" + summary.getTotalTaxDeductible() + " in tax-deductible business expenses ready for export.");
        }

        if (insights.isEmpty()) {
            insights.add("✨ Smart Forecast: Your spending is balanced. Keep logging expenses to generate predictive insights.");
        }

        return insights;
    }
}
