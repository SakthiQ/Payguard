package com.java.PayGuard.fraud.service;

import com.java.PayGuard.fraud.dto.FraudEvaluationResult;
import com.java.PayGuard.fraud.entity.FraudRule;
import com.java.PayGuard.fraud.repository.FraudRuleRepository;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEngineService {

    private final FraudRuleRepository fraudRuleRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public FraudEvaluationResult evaluateTransfer(Wallet senderWallet, BigDecimal amount) {
        // 1. Evaluate High Amount Rule
        Optional<FraudRule> highAmountRuleOpt = fraudRuleRepository.findByRuleCode("HIGH_AMOUNT");
        if (highAmountRuleOpt.isPresent() && Boolean.TRUE.equals(highAmountRuleOpt.get().getEnabled())) {
            FraudRule rule = highAmountRuleOpt.get();
            if (amount.compareTo(rule.getThresholdValue()) >= 0) {
                log.warn("HIGH_AMOUNT rule triggered for sender wallet {}: amount {} >= threshold {}",
                        senderWallet.getId(), amount, rule.getThresholdValue());
                return FraudEvaluationResult.builder()
                        .flagged(true)
                        .riskScore(85)
                        .triggeredRuleCode("HIGH_AMOUNT")
                        .flagReason("Transfer amount $" + amount + " exceeds threshold limit $" + rule.getThresholdValue())
                        .build();
            }
        }

        // 2. Evaluate Velocity Limit Rule
        Optional<FraudRule> velocityRuleOpt = fraudRuleRepository.findByRuleCode("HIGH_VELOCITY");
        if (velocityRuleOpt.isPresent() && Boolean.TRUE.equals(velocityRuleOpt.get().getEnabled())) {
            FraudRule rule = velocityRuleOpt.get();
            int windowSeconds = rule.getTimeWindowSeconds() != null ? rule.getTimeWindowSeconds() : 300;
            long maxCount = rule.getThresholdValue().longValue();

            LocalDateTime since = LocalDateTime.now().minusSeconds(windowSeconds);
            long recentTransferCount = transactionRepository.countRecentTransfersBySender(senderWallet.getId(), since);

            if (recentTransferCount >= maxCount) {
                log.warn("HIGH_VELOCITY rule triggered for sender wallet {}: count {} >= maxCount {}",
                        senderWallet.getId(), recentTransferCount, maxCount);
                return FraudEvaluationResult.builder()
                        .flagged(true)
                        .riskScore(90)
                        .triggeredRuleCode("HIGH_VELOCITY")
                        .flagReason("Sender wallet attempted " + (recentTransferCount + 1) + " transfers within " + windowSeconds + " seconds")
                        .build();
            }
        }

        return FraudEvaluationResult.builder()
                .flagged(false)
                .riskScore(0)
                .build();
    }
}
