package com.java.PayGuard.expense;

import com.java.PayGuard.common.exception.InsufficientBalanceException;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.expense.dto.ExpenseRequest;
import com.java.PayGuard.expense.dto.ExpenseResponse;
import com.java.PayGuard.expense.dto.ExpenseSummaryResponse;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.entity.ExpenseEntry;
import com.java.PayGuard.expense.repository.ExpenseRepository;
import com.java.PayGuard.expense.service.ExpenseService;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.entity.UserStatus;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExpenseService expenseService;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@payguard.io")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        testWallet = Wallet.builder()
                .id(10L)
                .user(testUser)
                .accountNumber("ACC-TEST-001")
                .balance(new BigDecimal("500.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: logExpense — CASH (no wallet debit)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should log a CASH expense entry without touching wallet balance")
    void logExpense_Cash_Success() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category(ExpenseCategory.FOOD)
                .accountType(AccountType.CASH)
                .amount(new BigDecimal("45.50"))
                .merchantName("Whole Foods Market")
                .description("Weekly groceries")
                .taxDeductible(false)
                .recurring(false)
                .build();

        ExpenseEntry savedEntry = ExpenseEntry.builder()
                .id(101L)
                .user(testUser)
                .accountType(AccountType.CASH)
                .category(ExpenseCategory.FOOD)
                .amount(new BigDecimal("45.5000"))
                .currency("USD")
                .merchantName("Whole Foods Market")
                .description("Weekly groceries")
                .taxDeductible(false)
                .recurring(false)
                .build();

        when(expenseRepository.save(any(ExpenseEntry.class))).thenReturn(savedEntry);

        ExpenseResponse response = expenseService.logExpense(request, testUser);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(AccountType.CASH, response.getAccountType());
        assertEquals(ExpenseCategory.FOOD, response.getCategory());
        assertEquals("Whole Foods Market", response.getMerchantName());

        // Wallet should NOT be touched for CASH transactions
        verifyNoInteractions(walletRepository);
        verify(expenseRepository, times(1)).save(any(ExpenseEntry.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: logExpense — WALLET (auto-debit PayGuard wallet)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should debit PayGuard wallet balance when accountType is WALLET")
    void logExpense_Wallet_AutoDebit_Success() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category(ExpenseCategory.ENTERTAINMENT)
                .accountType(AccountType.WALLET)
                .amount(new BigDecimal("120.00"))
                .merchantName("Netflix")
                .taxDeductible(false)
                .recurring(true)
                .build();

        ExpenseEntry savedEntry = ExpenseEntry.builder()
                .id(102L)
                .user(testUser)
                .accountType(AccountType.WALLET)
                .category(ExpenseCategory.ENTERTAINMENT)
                .amount(new BigDecimal("120.0000"))
                .currency("USD")
                .merchantName("Netflix")
                .recurring(true)
                .taxDeductible(false)
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByIdWithPessimisticWriteLock(10L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(expenseRepository.save(any(ExpenseEntry.class))).thenReturn(savedEntry);

        ExpenseResponse response = expenseService.logExpense(request, testUser);

        assertNotNull(response);
        assertEquals(AccountType.WALLET, response.getAccountType());

        // Wallet balance should have been debited by $120.00
        assertEquals(new BigDecimal("380.0000"), testWallet.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: logExpense — WALLET with insufficient balance
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw InsufficientBalanceException when wallet balance is too low")
    void logExpense_Wallet_InsufficientBalance_Throws() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category(ExpenseCategory.TRAVEL)
                .accountType(AccountType.WALLET)
                .amount(new BigDecimal("9999.00"))
                .merchantName("Emirates Airlines")
                .taxDeductible(true)
                .recurring(false)
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByIdWithPessimisticWriteLock(10L)).thenReturn(Optional.of(testWallet));

        assertThrows(InsufficientBalanceException.class,
                () -> expenseService.logExpense(request, testUser));

        // Wallet balance should remain unchanged
        assertEquals(new BigDecimal("500.0000"), testWallet.getBalance());
        verify(expenseRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getSummary — Multi-category totals & tax deductible calculation
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return correct totals and category breakdown in expense summary")
    void getSummary_CorrectTotalsAndCategoryBreakdown() {
        ExpenseEntry foodEntry = ExpenseEntry.builder()
                .id(1L).user(testUser)
                .category(ExpenseCategory.FOOD).accountType(AccountType.CASH)
                .amount(new BigDecimal("150.0000")).taxDeductible(false).recurring(false).build();

        ExpenseEntry businessEntry = ExpenseEntry.builder()
                .id(2L).user(testUser)
                .category(ExpenseCategory.BUSINESS).accountType(AccountType.CREDIT)
                .amount(new BigDecimal("300.0000")).taxDeductible(true).recurring(false).build();

        ExpenseEntry travelEntry = ExpenseEntry.builder()
                .id(3L).user(testUser)
                .category(ExpenseCategory.TRAVEL).accountType(AccountType.WALLET)
                .amount(new BigDecimal("200.0000")).taxDeductible(true).recurring(false).build();

        when(expenseRepository.findAllByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(foodEntry, businessEntry, travelEntry));

        ExpenseSummaryResponse summary = expenseService.getSummary(testUser);

        assertNotNull(summary);
        assertEquals(new BigDecimal("650.0000"), summary.getTotalExpenses());
        assertEquals(new BigDecimal("500.0000"), summary.getTotalTaxDeductible());
        assertEquals(new BigDecimal("150.0000"), summary.getCategoryBreakdown().get(ExpenseCategory.FOOD));
        assertEquals(new BigDecimal("300.0000"), summary.getCategoryBreakdown().get(ExpenseCategory.BUSINESS));
        assertEquals(new BigDecimal("200.0000"), summary.getCategoryBreakdown().get(ExpenseCategory.TRAVEL));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: deleteExpense — Owner can delete their own expense
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should allow user to delete their own expense entry")
    void deleteExpense_Owner_Success() {
        ExpenseEntry entry = ExpenseEntry.builder()
                .id(99L).user(testUser)
                .amount(new BigDecimal("50.0000"))
                .category(ExpenseCategory.BILLS).accountType(AccountType.CASH)
                .taxDeductible(false).recurring(false).build();

        when(expenseRepository.findById(99L)).thenReturn(Optional.of(entry));

        assertDoesNotThrow(() -> expenseService.deleteExpense(99L, testUser));
        verify(expenseRepository, times(1)).delete(entry);
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: deleteExpense — Foreign user cannot delete another user's expense
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw IllegalArgumentException when another user tries to delete a foreign expense")
    void deleteExpense_UnauthorizedUser_Throws() {
        User otherUser = User.builder()
                .id(99L).email("other@payguard.io")
                .role(Role.ROLE_CUSTOMER).status(UserStatus.ACTIVE).build();

        ExpenseEntry entry = ExpenseEntry.builder()
                .id(55L).user(otherUser)
                .amount(new BigDecimal("80.0000"))
                .category(ExpenseCategory.OTHER).accountType(AccountType.CASH)
                .taxDeductible(false).recurring(false).build();

        when(expenseRepository.findById(55L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class,
                () -> expenseService.deleteExpense(55L, testUser));

        verify(expenseRepository, never()).delete(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: deleteExpense — Not found throws ResourceNotFoundException
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw ResourceNotFoundException when expense ID does not exist")
    void deleteExpense_NotFound_Throws() {
        when(expenseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expenseService.deleteExpense(404L, testUser));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: logExpense — CREDIT card (no wallet debit, tracks as liability)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should log CREDIT expense without wallet interaction (liability tracking)")
    void logExpense_Credit_NoWalletDebit() {
        ExpenseRequest request = ExpenseRequest.builder()
                .category(ExpenseCategory.BUSINESS)
                .accountType(AccountType.CREDIT)
                .amount(new BigDecimal("500.00"))
                .merchantName("AWS Cloud Services")
                .taxDeductible(true)
                .recurring(true)
                .build();

        ExpenseEntry savedEntry = ExpenseEntry.builder()
                .id(201L).user(testUser)
                .accountType(AccountType.CREDIT).category(ExpenseCategory.BUSINESS)
                .amount(new BigDecimal("500.0000")).currency("USD")
                .merchantName("AWS Cloud Services").taxDeductible(true).recurring(true).build();

        when(expenseRepository.save(any(ExpenseEntry.class))).thenReturn(savedEntry);

        ExpenseResponse response = expenseService.logExpense(request, testUser);

        assertNotNull(response);
        assertTrue(response.getTaxDeductible());
        assertTrue(response.getRecurring());
        assertEquals("AWS Cloud Services", response.getMerchantName());

        // No wallet involvement for CREDIT account type
        verifyNoInteractions(walletRepository);
    }
}
