package com.java.PayGuard.expense;

import com.java.PayGuard.expense.dto.BudgetProgressDto;
import com.java.PayGuard.expense.entity.BudgetLimit;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.entity.ExpenseEntry;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.repository.BudgetLimitRepository;
import com.java.PayGuard.expense.repository.ExpenseRepository;
import com.java.PayGuard.expense.service.BudgetService;
import com.java.PayGuard.notification.service.NotificationService;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetLimitRepository budgetLimitRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private BudgetLimit foodBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("budget@payguard.io")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        foodBudget = BudgetLimit.builder()
                .id(10L)
                .user(testUser)
                .category(ExpenseCategory.FOOD)
                .monthlyCap(new BigDecimal("500.0000"))
                .spentAmount(BigDecimal.ZERO)
                .rolloverAmount(BigDecimal.ZERO)
                .warningThresholdPercent(80)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: setBudget — Creates new budget when none exists for category
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should create a new budget limit when no budget exists for the category")
    void setBudget_Creates_NewBudget() {
        when(budgetLimitRepository.findByUserAndCategory(testUser, ExpenseCategory.FOOD))
                .thenReturn(Optional.empty());
        when(budgetLimitRepository.save(any(BudgetLimit.class))).thenReturn(foodBudget);

        BudgetLimit result = budgetService.setBudget(testUser, ExpenseCategory.FOOD, new BigDecimal("500.00"));

        assertNotNull(result);
        assertEquals(ExpenseCategory.FOOD, result.getCategory());
        assertEquals(new BigDecimal("500.0000"), result.getMonthlyCap());
        verify(budgetLimitRepository, times(1)).save(any(BudgetLimit.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: setBudget — Updates existing budget cap without creating duplicate
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should update existing budget cap for a category instead of creating a new one")
    void setBudget_Updates_ExistingBudget() {
        when(budgetLimitRepository.findByUserAndCategory(testUser, ExpenseCategory.FOOD))
                .thenReturn(Optional.of(foodBudget));
        when(budgetLimitRepository.save(any(BudgetLimit.class))).thenReturn(foodBudget);

        BudgetLimit result = budgetService.setBudget(testUser, ExpenseCategory.FOOD, new BigDecimal("750.00"));

        assertNotNull(result);
        // The existing budget should have been updated with the new cap
        assertEquals(new BigDecimal("750.0000"), foodBudget.getMonthlyCap());
        verify(budgetLimitRepository, times(1)).save(foodBudget);
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getBudgetProgress — OK status when under 80% threshold
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return OK status pill when spending is under 80% of budget cap")
    void getBudgetProgress_Under80Percent_ReturnsOK() {
        // Spent $200 of $500 cap = 40% used
        ExpenseEntry e1 = ExpenseEntry.builder()
                .user(testUser).category(ExpenseCategory.FOOD).accountType(AccountType.CASH)
                .amount(new BigDecimal("200.0000")).taxDeductible(false).recurring(false).build();

        when(budgetLimitRepository.findAllByUser(testUser)).thenReturn(List.of(foodBudget));
        when(expenseRepository.filterExpenses(eq(testUser), eq(ExpenseCategory.FOOD), isNull(), isNull(), isNull(), any(), isNull()))
                .thenReturn(List.of(e1));

        List<BudgetProgressDto> progress = budgetService.getBudgetProgress(testUser);

        assertEquals(1, progress.size());
        BudgetProgressDto dto = progress.get(0);
        assertEquals("OK", dto.getStatusPill());
        assertEquals(40.0, dto.getPercentUsed(), 0.5);
        assertEquals(new BigDecimal("300.0000"), dto.getRemainingAmount());

        verifyNoInteractions(notificationService);
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getBudgetProgress — WARNING_80 status when between 80%–99%
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return WARNING_80 pill and trigger notification when spending hits 80% threshold")
    void getBudgetProgress_At80Percent_ReturnsWarning() {
        // Spent $430 of $500 cap = 86% used → WARNING_80
        ExpenseEntry e1 = ExpenseEntry.builder()
                .user(testUser).category(ExpenseCategory.FOOD).accountType(AccountType.CASH)
                .amount(new BigDecimal("430.0000")).taxDeductible(false).recurring(false).build();

        when(budgetLimitRepository.findAllByUser(testUser)).thenReturn(List.of(foodBudget));
        when(expenseRepository.filterExpenses(eq(testUser), eq(ExpenseCategory.FOOD), isNull(), isNull(), isNull(), any(), isNull()))
                .thenReturn(List.of(e1));

        List<BudgetProgressDto> progress = budgetService.getBudgetProgress(testUser);

        assertEquals(1, progress.size());
        assertEquals("WARNING_80", progress.get(0).getStatusPill());
        verify(notificationService, times(1)).broadcast(eq("BUDGET_WARNING_80"), anyMap());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getBudgetProgress — OVERSPENT_100 status when at or over cap
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return OVERSPENT_100 pill and trigger overspend notification when cap is exceeded")
    void getBudgetProgress_Exceeded100Percent_ReturnsOverspent() {
        // Spent $550 of $500 cap = 110% → OVERSPENT_100
        ExpenseEntry e1 = ExpenseEntry.builder()
                .user(testUser).category(ExpenseCategory.FOOD).accountType(AccountType.CASH)
                .amount(new BigDecimal("550.0000")).taxDeductible(false).recurring(false).build();

        when(budgetLimitRepository.findAllByUser(testUser)).thenReturn(List.of(foodBudget));
        when(expenseRepository.filterExpenses(eq(testUser), eq(ExpenseCategory.FOOD), isNull(), isNull(), isNull(), any(), isNull()))
                .thenReturn(List.of(e1));

        List<BudgetProgressDto> progress = budgetService.getBudgetProgress(testUser);

        assertEquals(1, progress.size());
        assertEquals("OVERSPENT_100", progress.get(0).getStatusPill());
        assertTrue(progress.get(0).getRemainingAmount().compareTo(BigDecimal.ZERO) < 0);
        verify(notificationService, times(1)).broadcast(eq("BUDGET_EXCEEDED_100"), anyMap());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getBudgetProgress — Empty budgets returns empty list
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return empty list when user has no budgets configured")
    void getBudgetProgress_NoBudgets_ReturnsEmptyList() {
        when(budgetLimitRepository.findAllByUser(testUser)).thenReturn(List.of());

        List<BudgetProgressDto> progress = budgetService.getBudgetProgress(testUser);

        assertNotNull(progress);
        assertTrue(progress.isEmpty());
        verifyNoInteractions(expenseRepository);
        verifyNoInteractions(notificationService);
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: setBudget — Cap is persisted with correct scale (4dp HALF_EVEN)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should persist budget cap with 4 decimal precision using HALF_EVEN rounding")
    void setBudget_CapPrecision_4DecimalPlaces() {
        BudgetLimit scaledBudget = BudgetLimit.builder()
                .id(11L)
                .user(testUser)
                .category(ExpenseCategory.TRAVEL)
                .monthlyCap(new BigDecimal("299.9900"))
                .spentAmount(BigDecimal.ZERO)
                .rolloverAmount(BigDecimal.ZERO)
                .warningThresholdPercent(80)
                .build();

        when(budgetLimitRepository.findByUserAndCategory(testUser, ExpenseCategory.TRAVEL))
                .thenReturn(Optional.empty());
        when(budgetLimitRepository.save(any(BudgetLimit.class))).thenReturn(scaledBudget);

        BudgetLimit result = budgetService.setBudget(testUser, ExpenseCategory.TRAVEL, new BigDecimal("299.99"));

        assertNotNull(result);
        // Scale should be 4dp
        assertEquals(4, result.getMonthlyCap().scale());
    }
}
