package com.java.PayGuard.fraud.service;

import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.InsufficientBalanceException;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.common.util.LockUtils;
import com.java.PayGuard.fraud.dto.FraudFlagResponse;
import com.java.PayGuard.fraud.dto.FraudReviewRequest;
import com.java.PayGuard.fraud.entity.FlagStatus;
import com.java.PayGuard.fraud.entity.FraudFlag;
import com.java.PayGuard.fraud.repository.FraudFlagRepository;
import com.java.PayGuard.transaction.entity.Transaction;
import com.java.PayGuard.transaction.entity.TransactionStatus;
import com.java.PayGuard.transaction.repository.TransactionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.java.PayGuard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
public class FraudReviewService {

    private final FraudFlagRepository fraudFlagRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final AiRiskExplanationService aiRiskExplanationService;

    @Transactional(readOnly = true)
    public List<FraudFlagResponse> getPendingFlags() {
        return fraudFlagRepository.findAllByStatus(FlagStatus.UNDER_REVIEW).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FraudFlagResponse getFlagById(Long id) {
        FraudFlag flag = fraudFlagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud flag not found with ID: " + id));
        return mapToResponse(flag);
    }

    @Transactional
    public FraudFlagResponse reviewFlag(Long flagId, FraudReviewRequest request, User reviewer) {
        FraudFlag flag = fraudFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud flag not found with ID: " + flagId));

        if (flag.getStatus() != FlagStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Fraud flag with ID " + flagId + " has already been reviewed (Status: " + flag.getStatus() + ").");
        }

        Transaction tx = flag.getTransaction();
        String action = request.getAction().trim().toUpperCase();

        if ("APPROVE".equals(action)) {
            Wallet sender = tx.getSenderWallet();
            Wallet recipient = tx.getReceiverWallet();
            BigDecimal amount = tx.getAmount().setScale(4, RoundingMode.HALF_EVEN);

            // Deterministic Lock Ordering: Sort IDs to acquire pessimistic write locks
            LockUtils.OrderedWalletIds ordered = LockUtils.sortWalletIds(sender.getId(), recipient.getId());

            Wallet firstLocked = walletRepository.findByIdWithPessimisticWriteLock(ordered.getFirstId()).orElseThrow();
            Wallet secondLocked = walletRepository.findByIdWithPessimisticWriteLock(ordered.getSecondId()).orElseThrow();

            Wallet lockedSender = firstLocked.getId().equals(sender.getId()) ? firstLocked : secondLocked;
            Wallet lockedRecipient = firstLocked.getId().equals(recipient.getId()) ? firstLocked : secondLocked;

            if (lockedSender.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Cannot approve transfer: Sender balance insufficient (Available: $" + lockedSender.getBalance() + ", Required: $" + amount + ")");
            }

            // Debit sender & Credit recipient
            lockedSender.setBalance(lockedSender.getBalance().subtract(amount).setScale(4, RoundingMode.HALF_EVEN));
            lockedRecipient.setBalance(lockedRecipient.getBalance().add(amount).setScale(4, RoundingMode.HALF_EVEN));

            walletRepository.save(lockedSender);
            walletRepository.save(lockedRecipient);

            tx.setStatus(TransactionStatus.COMPLETED);
            flag.setStatus(FlagStatus.APPROVED);

            publishAuditEvent(reviewer, "FRAUD_APPROVED", flag.getId().toString(), Map.of(
                    "transactionReference", tx.getTransactionReference(),
                    "amount", amount.toString(),
                    "reviewerNotes", java.util.Objects.toString(request.getNotes(), "")
            ));

            log.info("Fraud analyst {} APPROVED flagged transaction {}", reviewer.getEmail(), tx.getTransactionReference());

        } else if ("DECLINE".equals(action)) {
            tx.setStatus(TransactionStatus.DECLINED);
            flag.setStatus(FlagStatus.DECLINED);

            publishAuditEvent(reviewer, "FRAUD_DECLINED", flag.getId().toString(), Map.of(
                    "transactionReference", tx.getTransactionReference(),
                    "amount", tx.getAmount().toString(),
                    "reviewerNotes", java.util.Objects.toString(request.getNotes(), "")
            ));

            log.info("Fraud analyst {} DECLINED flagged transaction {}", reviewer.getEmail(), tx.getTransactionReference());

        } else {
            throw new IllegalArgumentException("Invalid review action: " + request.getAction() + ". Must be APPROVE or DECLINE.");
        }

        transactionRepository.save(tx);

        flag.setReviewer(reviewer);
        flag.setReviewNotes(request.getNotes());
        flag.setReviewedAt(LocalDateTime.now());

        FraudFlag savedFlag = fraudFlagRepository.save(flag);
        FraudFlagResponse response = mapToResponse(savedFlag);

        notificationService.broadcast("FRAUD_REVIEW_COMPLETED", Map.of(
                "flagId", savedFlag.getId(),
                "transactionReference", tx.getTransactionReference(),
                "status", savedFlag.getStatus().name(),
                "reviewerEmail", reviewer.getEmail(),
                "action", action
        ));

        return response;
    }

    private void publishAuditEvent(User reviewer, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(reviewer.getId())
                .actorEmail(reviewer.getEmail())
                .actorRole(reviewer.getRole().name())
                .resourceType("FRAUD_FLAG")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }

    private FraudFlagResponse mapToResponse(FraudFlag flag) {
        Transaction tx = flag.getTransaction();
        return FraudFlagResponse.builder()
                .flagId(flag.getId())
                .transactionId(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .senderAccountNumber(tx.getSenderWallet().getAccountNumber())
                .recipientAccountNumber(tx.getReceiverWallet().getAccountNumber())
                .amount(tx.getAmount())
                .triggeredRuleCode(flag.getTriggeredRuleCode())
                .riskScore(flag.getRiskScore())
                .flagReason(flag.getFlagReason())
                .aiRiskInsight(aiRiskExplanationService.generateRiskInsight(flag))
                .status(flag.getStatus().name())
                .reviewerEmail(flag.getReviewer() != null ? flag.getReviewer().getEmail() : null)
                .reviewNotes(flag.getReviewNotes())
                .reviewedAt(flag.getReviewedAt())
                .createdAt(flag.getCreatedAt())
                .build();
    }
}
