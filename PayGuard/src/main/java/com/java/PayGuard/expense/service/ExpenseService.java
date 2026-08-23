package com.java.PayGuard.expense.service;

import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.InsufficientBalanceException;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.expense.dto.ExpenseRequest;
import com.java.PayGuard.expense.dto.ExpenseResponse;
import com.java.PayGuard.expense.dto.ExpenseSummaryResponse;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.entity.ExpenseEntry;
import com.java.PayGuard.expense.repository.ExpenseRepository;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExpenseResponse logExpense(ExpenseRequest request, User currentUser) {
        BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);
        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "USD";

        // If account type is WALLET, verify available balance and debit PayGuard digital wallet
        if (request.getAccountType() == AccountType.WALLET) {
            Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + currentUser.getEmail()));

            Wallet lockedWallet = walletRepository.findByIdWithPessimisticWriteLock(wallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

            if (!"ACTIVE".equalsIgnoreCase(lockedWallet.getStatus().name())) {
                throw new IllegalStateException("Wallet is not ACTIVE (Status: " + lockedWallet.getStatus() + ")");
            }

            if (lockedWallet.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Insufficient wallet balance for expense (Available: $" + lockedWallet.getBalance() + ", Required: $" + amount + ")");
            }

            // Debit wallet
            lockedWallet.setBalance(lockedWallet.getBalance().subtract(amount).setScale(4, RoundingMode.HALF_EVEN));
            walletRepository.save(lockedWallet);
            log.info("Debited wallet ID {} by ${} for expense category {}", lockedWallet.getId(), amount, request.getCategory());
        }

        ExpenseEntry entry = ExpenseEntry.builder()
                .user(currentUser)
                .accountType(request.getAccountType())
                .category(request.getCategory())
                .amount(amount)
                .currency(currency)
                .merchantName(request.getMerchantName() != null ? request.getMerchantName().trim() : "General Merchant")
                .description(request.getDescription())
                .tags(request.getTags())
                .taxDeductible(Boolean.TRUE.equals(request.getTaxDeductible()))
                .recurring(Boolean.TRUE.equals(request.getRecurring()))
                .activityInvoiceId(request.getActivityInvoiceId())
                .build();

        ExpenseEntry saved = expenseRepository.save(entry);

        // Audit Event
        publishAuditEvent(currentUser, "EXPENSE_LOGGED", saved.getId().toString(), Map.of(
                "amount", amount.toString(),
                "category", request.getCategory().name(),
                "accountType", request.getAccountType().name(),
                "merchantName", saved.getMerchantName()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(User currentUser,
                                             ExpenseCategory category,
                                             AccountType accountType,
                                             Boolean taxDeductible,
                                             String query,
                                             LocalDateTime startDate,
                                             LocalDateTime endDate) {
        return expenseRepository.filterExpenses(currentUser, category, accountType, taxDeductible, query, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getSummary(User currentUser) {
        List<ExpenseEntry> all = expenseRepository.findAllByUserOrderByCreatedAtDesc(currentUser);

        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalTaxDeductible = BigDecimal.ZERO;
        Map<ExpenseCategory, BigDecimal> categoryMap = new EnumMap<>(ExpenseCategory.class);
        Map<AccountType, BigDecimal> accountMap = new EnumMap<>(AccountType.class);

        for (ExpenseEntry e : all) {
            totalExpenses = totalExpenses.add(e.getAmount());
            if (Boolean.TRUE.equals(e.getTaxDeductible())) {
                totalTaxDeductible = totalTaxDeductible.add(e.getAmount());
            }

            categoryMap.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
            accountMap.merge(e.getAccountType(), e.getAmount(), BigDecimal::add);
        }

        return ExpenseSummaryResponse.builder()
                .totalExpenses(totalExpenses)
                .totalTaxDeductible(totalTaxDeductible)
                .categoryBreakdown(categoryMap)
                .accountTypeBreakdown(accountMap)
                .build();
    }

    @Transactional
    public void deleteExpense(Long id, User currentUser) {
        ExpenseEntry entry = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        if (!entry.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this expense.");
        }

        expenseRepository.delete(entry);
    }

    private void publishAuditEvent(User user, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .resourceType("EXPENSE")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }

    private ExpenseResponse mapToResponse(ExpenseEntry entry) {
        return ExpenseResponse.builder()
                .id(entry.getId())
                .userId(entry.getUser().getId())
                .userEmail(entry.getUser().getEmail())
                .accountType(entry.getAccountType())
                .category(entry.getCategory())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .merchantName(entry.getMerchantName())
                .description(entry.getDescription())
                .tags(entry.getTags())
                .taxDeductible(entry.getTaxDeductible())
                .recurring(entry.getRecurring())
                .activityInvoiceId(entry.getActivityInvoiceId())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
