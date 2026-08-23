package com.java.PayGuard.admin.service;

import com.java.PayGuard.admin.dto.AdminUserResponse;
import com.java.PayGuard.admin.dto.FraudRuleResponse;
import com.java.PayGuard.admin.dto.UpdateFraudRuleRequest;
import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.fraud.entity.FraudRule;
import com.java.PayGuard.fraud.repository.FraudRuleRepository;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.repository.UserRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminUserResponse freezeWallet(Long walletId, User admin) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));

        wallet.setStatus(WalletStatus.FROZEN);
        Wallet savedWallet = walletRepository.save(wallet);

        publishAuditEvent(admin, "WALLET_FROZEN", savedWallet.getId().toString(), Map.of(
                "accountNumber", savedWallet.getAccountNumber(),
                "userEmail", savedWallet.getUser().getEmail()
        ));

        log.info("Admin {} FROZE wallet ID {}", admin.getEmail(), walletId);
        return mapToAdminUserResponse(savedWallet.getUser());
    }

    @Transactional
    public AdminUserResponse unfreezeWallet(Long walletId, User admin) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with ID: " + walletId));

        wallet.setStatus(WalletStatus.ACTIVE);
        Wallet savedWallet = walletRepository.save(wallet);

        publishAuditEvent(admin, "WALLET_UNFROZEN", savedWallet.getId().toString(), Map.of(
                "accountNumber", savedWallet.getAccountNumber(),
                "userEmail", savedWallet.getUser().getEmail()
        ));

        log.info("Admin {} UNFROZE wallet ID {}", admin.getEmail(), walletId);
        return mapToAdminUserResponse(savedWallet.getUser());
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponse> getAllFraudRules() {
        return fraudRuleRepository.findAll().stream()
                .map(this::mapToFraudRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FraudRuleResponse updateFraudRule(String ruleCode, UpdateFraudRuleRequest request, User admin) {
        FraudRule rule = fraudRuleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found with code: " + ruleCode));

        rule.setThresholdValue(request.getThresholdValue());
        rule.setTimeWindowSeconds(request.getTimeWindowSeconds());
        rule.setEnabled(request.getEnabled());

        FraudRule savedRule = fraudRuleRepository.save(rule);

        publishAuditEvent(admin, "FRAUD_RULE_UPDATED", savedRule.getRuleCode(), Map.of(
                "thresholdValue", savedRule.getThresholdValue().toString(),
                "enabled", savedRule.getEnabled().toString()
        ));

        log.info("Admin {} updated fraud rule {}", admin.getEmail(), ruleCode);
        return mapToFraudRuleResponse(savedRule);
    }

    private void publishAuditEvent(User admin, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(admin.getId())
                .actorEmail(admin.getEmail())
                .actorRole(admin.getRole().name())
                .resourceType("ADMIN_ACTION")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);
        return AdminUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .userStatus(user.getStatus() != null ? user.getStatus().name() : null)
                .walletId(wallet != null ? wallet.getId() : null)
                .accountNumber(wallet != null ? wallet.getAccountNumber() : null)
                .balance(wallet != null ? wallet.getBalance() : null)
                .walletStatus(wallet != null && wallet.getStatus() != null ? wallet.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private FraudRuleResponse mapToFraudRuleResponse(FraudRule rule) {
        return FraudRuleResponse.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .ruleName(rule.getRuleName())
                .description(rule.getDescription())
                .thresholdValue(rule.getThresholdValue())
                .timeWindowSeconds(rule.getTimeWindowSeconds())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
