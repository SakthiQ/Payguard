package com.java.PayGuard.expense.service;

import com.java.PayGuard.expense.dto.BudgetProgressDto;
import com.java.PayGuard.expense.entity.BudgetLimit;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.repository.BudgetLimitRepository;
import com.java.PayGuard.expense.repository.ExpenseRepository;
import com.java.PayGuard.notification.service.NotificationService;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetLimitRepository budgetLimitRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    @Transactional
    public BudgetLimit setBudget(User currentUser, ExpenseCategory category, BigDecimal monthlyCap) {
        BigDecimal cap = monthlyCap.setScale(4, RoundingMode.HALF_EVEN);

        Optional<BudgetLimit> existing = budgetLimitRepository.findByUserAndCategory(currentUser, category);
        BudgetLimit limit;

        if (existing.isPresent()) {
            limit = existing.get();
            limit.setMonthlyCap(cap);
        } else {
            limit = BudgetLimit.builder()
                    .user(currentUser)
                    .category(category)
                    .monthlyCap(cap)
                    .spentAmount(BigDecimal.ZERO)
                    .rolloverAmount(BigDecimal.ZERO)
                    .warningThresholdPercent(80)
                    .build();
        }

        return budgetLimitRepository.save(limit);
    }

    @Transactional(readOnly = true)
    public List<BudgetProgressDto> getBudgetProgress(User currentUser) {
        List<BudgetLimit> limits = budgetLimitRepository.findAllByUser(currentUser);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        List<BudgetProgressDto> results = new ArrayList<>();

        for (BudgetLimit limit : limits) {
            // Sum month-to-date expenses for this category
            BigDecimal monthSpent = expenseRepository.filterExpenses(currentUser, limit.getCategory(), null, null, null, startOfMonth, null)
                    .stream()
                    .map(e -> e.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal effectiveCap = limit.getMonthlyCap().add(limit.getRolloverAmount());
            BigDecimal remaining = effectiveCap.subtract(monthSpent);

            double percentUsed = 0.0;
            if (effectiveCap.compareTo(BigDecimal.ZERO) > 0) {
                percentUsed = monthSpent.multiply(new BigDecimal("100"))
                        .divide(effectiveCap, 2, RoundingMode.HALF_EVEN)
                        .doubleValue();
            }

            String statusPill = "OK";
            if (percentUsed >= 100.0) {
                statusPill = "OVERSPENT_100";
                notificationService.broadcast("BUDGET_EXCEEDED_100", Map.of(
                        "category", limit.getCategory().name(),
                        "spent", monthSpent.toString(),
                        "cap", effectiveCap.toString()
                ));
            } else if (percentUsed >= limit.getWarningThresholdPercent()) {
                statusPill = "WARNING_80";
                notificationService.broadcast("BUDGET_WARNING_80", Map.of(
                        "category", limit.getCategory().name(),
                        "percentUsed", percentUsed,
                        "spent", monthSpent.toString()
                ));
            }

            results.add(BudgetProgressDto.builder()
                    .id(limit.getId())
                    .category(limit.getCategory())
                    .monthlyCap(limit.getMonthlyCap())
                    .spentAmount(monthSpent)
                    .rolloverAmount(limit.getRolloverAmount())
                    .remainingAmount(remaining)
                    .percentUsed(percentUsed)
                    .statusPill(statusPill)
                    .build());
        }

        return results;
    }
}
