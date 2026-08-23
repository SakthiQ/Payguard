package com.java.PayGuard.fraud;

import com.java.PayGuard.fraud.dto.FraudEvaluationResult;
import com.java.PayGuard.fraud.entity.FraudRule;
import com.java.PayGuard.fraud.repository.FraudRuleRepository;
import com.java.PayGuard.fraud.service.FraudEngineService;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudEngineServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudEngineService fraudEngineService;

    private Wallet senderWallet;
    private FraudRule highAmountRule;
    private FraudRule velocityRule;

    @BeforeEach
    void setUp() {
        senderWallet = Wallet.builder()
                .id(1L)
                .accountNumber("ACC-1001")
                .build();

        highAmountRule = FraudRule.builder()
                .id(1L)
                .ruleCode("HIGH_AMOUNT")
                .ruleName("High Amount Rule")
                .thresholdValue(new BigDecimal("5000.0000"))
                .enabled(true)
                .build();

        velocityRule = FraudRule.builder()
                .id(2L)
                .ruleCode("HIGH_VELOCITY")
                .ruleName("High Velocity Rule")
                .thresholdValue(new BigDecimal("3.0000"))
                .timeWindowSeconds(300)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should return low risk (not flagged) when transfer amount and velocity are within normal limits")
    void evaluateTransfer_LowRisk() {
        when(fraudRuleRepository.findByRuleCode("HIGH_AMOUNT")).thenReturn(Optional.of(highAmountRule));
        when(fraudRuleRepository.findByRuleCode("HIGH_VELOCITY")).thenReturn(Optional.of(velocityRule));
        when(transactionRepository.countRecentTransfersBySender(eq(1L), any(LocalDateTime.class))).thenReturn(1L);

        FraudEvaluationResult result = fraudEngineService.evaluateTransfer(senderWallet, new BigDecimal("250.0000"));

        assertFalse(result.isFlagged());
        assertEquals(0, result.getRiskScore());
        assertNull(result.getTriggeredRuleCode());
    }

    @Test
    @DisplayName("Should flag transfer when amount equals or exceeds HIGH_AMOUNT threshold")
    void evaluateTransfer_TriggerHighAmount() {
        when(fraudRuleRepository.findByRuleCode("HIGH_AMOUNT")).thenReturn(Optional.of(highAmountRule));

        FraudEvaluationResult result = fraudEngineService.evaluateTransfer(senderWallet, new BigDecimal("5000.0000"));

        assertTrue(result.isFlagged());
        assertEquals(85, result.getRiskScore());
        assertEquals("HIGH_AMOUNT", result.getTriggeredRuleCode());
        assertTrue(result.getFlagReason().contains("exceeds threshold limit"));
    }

    @Test
    @DisplayName("Should flag transfer when transfer frequency reaches or exceeds HIGH_VELOCITY threshold")
    void evaluateTransfer_TriggerHighVelocity() {
        when(fraudRuleRepository.findByRuleCode("HIGH_AMOUNT")).thenReturn(Optional.of(highAmountRule));
        when(fraudRuleRepository.findByRuleCode("HIGH_VELOCITY")).thenReturn(Optional.of(velocityRule));
        when(transactionRepository.countRecentTransfersBySender(eq(1L), any(LocalDateTime.class))).thenReturn(3L);

        FraudEvaluationResult result = fraudEngineService.evaluateTransfer(senderWallet, new BigDecimal("100.0000"));

        assertTrue(result.isFlagged());
        assertEquals(90, result.getRiskScore());
        assertEquals("HIGH_VELOCITY", result.getTriggeredRuleCode());
        assertTrue(result.getFlagReason().contains("transfers within 300 seconds"));
    }
}
