package com.java.PayGuard.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.AuthorizationException;
import com.java.PayGuard.common.exception.InsufficientBalanceException;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.common.util.LockUtils;
import com.java.PayGuard.fraud.dto.FraudEvaluationResult;
import com.java.PayGuard.fraud.entity.FlagStatus;
import com.java.PayGuard.fraud.entity.FraudFlag;
import com.java.PayGuard.fraud.repository.FraudFlagRepository;
import com.java.PayGuard.fraud.service.FraudEngineService;
import com.java.PayGuard.transaction.dto.TransactionResponse;
import com.java.PayGuard.transaction.dto.TransferRequest;
import com.java.PayGuard.transaction.dto.TransferResponse;
import com.java.PayGuard.transaction.entity.*;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final IdempotencyService idempotencyService;
    private final FraudEngineService fraudEngineService;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResponseEntity<TransferResponse> processTransfer(TransferRequest request, String idempotencyKeyHeader, User senderUser) {
        // 1. Acquire & Check Idempotency Key
        IdempotencyKeyEntity keyEntity = idempotencyService.acquireKey(idempotencyKeyHeader, senderUser);

        if (keyEntity.getStatus() == IdempotencyState.COMPLETED) {
            log.info("Idempotency hit: Returning cached response for key {}", idempotencyKeyHeader);
            try {
                TransferResponse cachedResponse = objectMapper.readValue(keyEntity.getResponseBody(), TransferResponse.class);
                return ResponseEntity.status(keyEntity.getResponseCode()).body(cachedResponse);
            } catch (Exception ex) {
                log.error("Failed to deserialize cached idempotency response", ex);
            }
        }

        // 2. Validate Sender Wallet
        Wallet senderWallet = walletRepository.findByUserId(senderUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found for user: " + senderUser.getEmail()));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AuthorizationException("Sender wallet is not ACTIVE (Status: " + senderWallet.getStatus() + ")");
        }

        // 3. Validate Recipient Wallet
        Wallet recipientWallet = walletRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient account not found: " + request.getRecipientAccountNumber()));

        if (senderWallet.getId().equals(recipientWallet.getId())) {
            throw new IllegalArgumentException("Cannot transfer funds to your own wallet.");
        }

        if (recipientWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AuthorizationException("Recipient wallet is not ACTIVE (Status: " + recipientWallet.getStatus() + ")");
        }

        BigDecimal transferAmount = request.getAmount().setScale(4, RoundingMode.HALF_EVEN);

        // 4. Run Deterministic Fraud Engine Evaluation
        FraudEvaluationResult fraudResult = fraudEngineService.evaluateTransfer(senderWallet, transferAmount);

        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        if (fraudResult.isFlagged()) {
            // Persist Flagged Transaction
            Transaction flaggedTx = Transaction.builder()
                    .transactionReference(reference)
                    .senderWallet(senderWallet)
                    .receiverWallet(recipientWallet)
                    .amount(transferAmount)
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.FLAGGED)
                    .idempotencyKey(keyEntity)
                    .description(request.getDescription())
                    .build();

            Transaction savedTx = transactionRepository.save(flaggedTx);

            // Create Fraud Flag entry for Analyst Queue
            FraudFlag flag = FraudFlag.builder()
                    .transaction(savedTx)
                    .triggeredRuleCode(fraudResult.getTriggeredRuleCode())
                    .riskScore(fraudResult.getRiskScore())
                    .flagReason(fraudResult.getFlagReason())
                    .status(FlagStatus.UNDER_REVIEW)
                    .build();

            fraudFlagRepository.save(flag);

            // Publish Async Domain Audit Event
            publishAuditEvent(senderUser, "TRANSFER_FLAGGED", reference, Map.of(
                    "reference", reference,
                    "amount", transferAmount.toString(),
                    "triggeredRule", fraudResult.getTriggeredRuleCode(),
                    "reason", fraudResult.getFlagReason()
            ));

            TransferResponse response = TransferResponse.builder()
                    .transactionReference(reference)
                    .senderAccountNumber(senderWallet.getAccountNumber())
                    .recipientAccountNumber(recipientWallet.getAccountNumber())
                    .amount(transferAmount)
                    .status(TransactionStatus.FLAGGED.name())
                    .message("Transaction flagged for risk evaluation: " + fraudResult.getFlagReason())
                    .createdAt(LocalDateTime.now())
                    .build();

            cacheIdempotencyResponse(keyEntity, HttpStatus.ACCEPTED.value(), response);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        // 5. Low Risk — Execute Payment with Deterministic Lock Ordering
        LockUtils.OrderedWalletIds ordered = LockUtils.sortWalletIds(senderWallet.getId(), recipientWallet.getId());

        // Lock lower ID first, then higher ID
        Wallet firstLocked = walletRepository.findByIdWithPessimisticWriteLock(ordered.getFirstId()).orElseThrow();
        Wallet secondLocked = walletRepository.findByIdWithPessimisticWriteLock(ordered.getSecondId()).orElseThrow();

        Wallet lockedSender = firstLocked.getId().equals(senderWallet.getId()) ? firstLocked : secondLocked;
        Wallet lockedRecipient = firstLocked.getId().equals(recipientWallet.getId()) ? firstLocked : secondLocked;

        // Verify balance under pessimistic lock
        if (lockedSender.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds. Available: $" + lockedSender.getBalance() + ", Requested: $" + transferAmount);
        }

        // Execute atomic debit and credit
        lockedSender.setBalance(lockedSender.getBalance().subtract(transferAmount).setScale(4, RoundingMode.HALF_EVEN));
        lockedRecipient.setBalance(lockedRecipient.getBalance().add(transferAmount).setScale(4, RoundingMode.HALF_EVEN));

        walletRepository.save(lockedSender);
        walletRepository.save(lockedRecipient);

        // Persist Completed Transaction
        Transaction completedTx = Transaction.builder()
                .transactionReference(reference)
                .senderWallet(lockedSender)
                .receiverWallet(lockedRecipient)
                .amount(transferAmount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(keyEntity)
                .description(request.getDescription())
                .build();

        Transaction savedTx = transactionRepository.save(completedTx);

        // Publish Async Domain Audit Event
        publishAuditEvent(senderUser, "TRANSFER_COMPLETED", reference, Map.of(
                "reference", reference,
                "senderAccount", lockedSender.getAccountNumber(),
                "recipientAccount", lockedRecipient.getAccountNumber(),
                "amount", transferAmount.toString()
        ));

        TransferResponse response = TransferResponse.builder()
                .transactionReference(reference)
                .senderAccountNumber(lockedSender.getAccountNumber())
                .recipientAccountNumber(lockedRecipient.getAccountNumber())
                .amount(transferAmount)
                .status(TransactionStatus.COMPLETED.name())
                .message("Transfer completed successfully.")
                .createdAt(LocalDateTime.now())
                .build();

        cacheIdempotencyResponse(keyEntity, HttpStatus.OK.value(), response);
        return ResponseEntity.ok(response);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(User authenticatedUser) {
        Wallet wallet = walletRepository.findByUserId(authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + authenticatedUser.getEmail()));

        return transactionRepository.findAllByWalletId(wallet.getId()).stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id, User authenticatedUser) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));

        Wallet userWallet = walletRepository.findByUserId(authenticatedUser.getId()).orElse(null);
        boolean isSender = userWallet != null && tx.getSenderWallet().getId().equals(userWallet.getId());
        boolean isReceiver = userWallet != null && tx.getReceiverWallet().getId().equals(userWallet.getId());

        if (!isSender && !isReceiver && authenticatedUser.getRole() != com.java.PayGuard.user.entity.Role.ROLE_ADMIN) {
            throw new AuthorizationException("You are not authorized to view this transaction.");
        }

        return mapToTransactionResponse(tx);
    }

    private void cacheIdempotencyResponse(IdempotencyKeyEntity keyEntity, int status, TransferResponse response) {
        try {
            objectMapper.findAndRegisterModules();
            String json = objectMapper.writeValueAsString(response);
            idempotencyService.markCompleted(keyEntity, status, json);
        } catch (Exception ex) {
            log.error("Failed to serialize idempotency response", ex);
        }
    }

    private void publishAuditEvent(User actor, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(actor.getId())
                .actorEmail(actor.getEmail())
                .actorRole(actor.getRole().name())
                .resourceType("TRANSACTION")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }

    private TransactionResponse mapToTransactionResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .senderAccountNumber(tx.getSenderWallet().getAccountNumber())
                .recipientAccountNumber(tx.getReceiverWallet().getAccountNumber())
                .amount(tx.getAmount())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
