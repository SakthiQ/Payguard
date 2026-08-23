package com.java.PayGuard.wallet.service;

import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.AuthorizationException;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.transaction.entity.Transaction;
import com.java.PayGuard.transaction.entity.TransactionStatus;
import com.java.PayGuard.transaction.entity.TransactionType;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.wallet.dto.DepositRequest;
import com.java.PayGuard.wallet.dto.DepositResponse;
import com.java.PayGuard.wallet.dto.WalletResponse;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public WalletResponse getWalletForCurrentUser(User authenticatedUser) {
        Wallet wallet = walletRepository.findByUserId(authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + authenticatedUser.getEmail()));
        return mapToResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId, User authenticatedUser) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));

        validateWalletOwnership(wallet, authenticatedUser);
        return mapToResponse(wallet);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBalance(Long walletId, User authenticatedUser) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));

        validateWalletOwnership(wallet, authenticatedUser);

        return Map.of(
                "walletId", wallet.getId(),
                "accountNumber", wallet.getAccountNumber(),
                "balance", wallet.getBalance(),
                "currency", wallet.getCurrency(),
                "status", wallet.getStatus().name()
        );
    }

    @Transactional
    public DepositResponse deposit(Long walletId, DepositRequest request, User authenticatedUser) {
        // Acquire Pessimistic Write Lock on target wallet
        Wallet wallet = walletRepository.findByIdWithPessimisticWriteLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));

        validateWalletOwnership(wallet, authenticatedUser);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AuthorizationException("Cannot deposit into wallet with status: " + wallet.getStatus());
        }

        BigDecimal depositAmount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal newBalance = wallet.getBalance().add(depositAmount).setScale(4, RoundingMode.HALF_EVEN);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        log.info("Deposit completed: walletId={}, amount={}, newBalance={}",
                wallet.getId(), depositAmount, newBalance);

        String reference = "DEP-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Transaction transaction = Transaction.builder()
                .transactionReference(reference)
                .senderWallet(wallet)
                .receiverWallet(wallet)
                .amount(depositAmount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description("Simulated Sandbox Deposit")
                .build();

        transactionRepository.save(transaction);

        // Emit Async Domain Audit Event
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + java.util.UUID.randomUUID())
                .eventType("WALLET_TOPUP")
                .actorUserId(authenticatedUser.getId())
                .actorEmail(authenticatedUser.getEmail())
                .actorRole(authenticatedUser.getRole().name())
                .resourceType("WALLET")
                .resourceId(wallet.getId().toString())
                .payload(Map.of(
                        "amount", depositAmount.toString(),
                        "newBalance", newBalance.toString(),
                        "transactionReference", reference
                ))
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishEvent(auditEvent);

        return DepositResponse.builder()
                .walletId(wallet.getId())
                .accountNumber(wallet.getAccountNumber())
                .newBalance(newBalance)
                .transactionReference(reference)
                .message("Deposit of $" + depositAmount + " completed successfully.")
                .build();
    }

    private void validateWalletOwnership(Wallet wallet, User authenticatedUser) {
        boolean isOwner = wallet.getUser().getId().equals(authenticatedUser.getId());
        boolean isAdmin = authenticatedUser.getRole() == Role.ROLE_ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized wallet access attempt: userId={} tried to access walletId={}",
                    authenticatedUser.getId(), wallet.getId());
            throw new AuthorizationException("You are not authorized to perform operations on this wallet.");
        }
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .accountNumber(wallet.getAccountNumber())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .userId(wallet.getUser().getId())
                .userEmail(wallet.getUser().getEmail())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
