package com.java.PayGuard.fraud;

import com.java.PayGuard.fraud.dto.FraudFlagResponse;
import com.java.PayGuard.fraud.dto.FraudReviewRequest;
import com.java.PayGuard.fraud.entity.FlagStatus;
import com.java.PayGuard.fraud.entity.FraudFlag;
import com.java.PayGuard.fraud.repository.FraudFlagRepository;
import com.java.PayGuard.fraud.service.FraudReviewService;
import com.java.PayGuard.transaction.entity.Transaction;
import com.java.PayGuard.transaction.entity.TransactionStatus;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.wallet.entity.Wallet;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.java.PayGuard.fraud.service.AiRiskExplanationService;
import com.java.PayGuard.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class FraudReviewServiceTest {

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AiRiskExplanationService aiRiskExplanationService;

    @InjectMocks
    private FraudReviewService fraudReviewService;

    private User reviewerUser;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private Transaction transaction;
    private FraudFlag fraudFlag;

    @BeforeEach
    void setUp() {
        reviewerUser = User.builder()
                .id(99L)
                .email("analyst@example.com")
                .role(Role.ROLE_FRAUD_ANALYST)
                .build();

        senderWallet = Wallet.builder()
                .id(1L)
                .accountNumber("ACC-SENDER")
                .balance(new BigDecimal("1000.0000"))
                .build();

        recipientWallet = Wallet.builder()
                .id(2L)
                .accountNumber("ACC-RECIPIENT")
                .balance(new BigDecimal("500.0000"))
                .build();

        transaction = Transaction.builder()
                .id(10L)
                .transactionReference("TXN-FLAGGED-01")
                .senderWallet(senderWallet)
                .receiverWallet(recipientWallet)
                .amount(new BigDecimal("300.0000"))
                .status(TransactionStatus.FLAGGED)
                .build();

        fraudFlag = FraudFlag.builder()
                .id(5L)
                .transaction(transaction)
                .triggeredRuleCode("HIGH_AMOUNT")
                .riskScore(85)
                .flagReason("Amount $300 exceeds limit")
                .status(FlagStatus.UNDER_REVIEW)
                .build();
    }

    @Test
    @DisplayName("Should approve flagged transaction and execute atomic balance transfer")
    void reviewFlag_Approve() {
        FraudReviewRequest request = FraudReviewRequest.builder()
                .action("APPROVE")
                .notes("Verified identity over phone call")
                .build();

        when(fraudFlagRepository.findById(5L)).thenReturn(Optional.of(fraudFlag));
        when(walletRepository.findByIdWithPessimisticWriteLock(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdWithPessimisticWriteLock(2L)).thenReturn(Optional.of(recipientWallet));
        when(fraudFlagRepository.save(any(FraudFlag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiRiskExplanationService.generateRiskInsight(any())).thenReturn("AI insight test");

        FraudFlagResponse response = fraudReviewService.reviewFlag(5L, request, reviewerUser);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("analyst@example.com", response.getReviewerEmail());
        assertEquals("Verified identity over phone call", response.getReviewNotes());

        // Sender debited, recipient credited
        assertEquals(new BigDecimal("700.0000"), senderWallet.getBalance());
        assertEquals(new BigDecimal("800.0000"), recipientWallet.getBalance());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
    }

    @Test
    @DisplayName("Should decline flagged transaction without modifying balances")
    void reviewFlag_Decline() {
        FraudReviewRequest request = FraudReviewRequest.builder()
                .action("DECLINE")
                .notes("Suspicious activity confirmed")
                .build();

        when(fraudFlagRepository.findById(5L)).thenReturn(Optional.of(fraudFlag));
        when(fraudFlagRepository.save(any(FraudFlag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiRiskExplanationService.generateRiskInsight(any())).thenReturn("AI insight test");

        FraudFlagResponse response = fraudReviewService.reviewFlag(5L, request, reviewerUser);

        assertNotNull(response);
        assertEquals("DECLINED", response.getStatus());

        // Balances remain untouched
        assertEquals(new BigDecimal("1000.0000"), senderWallet.getBalance());
        assertEquals(new BigDecimal("500.0000"), recipientWallet.getBalance());
        assertEquals(TransactionStatus.DECLINED, transaction.getStatus());
    }
}
