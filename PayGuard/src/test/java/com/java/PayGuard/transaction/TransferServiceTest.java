package com.java.PayGuard.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.PayGuard.common.exception.InsufficientBalanceException;
import com.java.PayGuard.fraud.dto.FraudEvaluationResult;
import com.java.PayGuard.fraud.repository.FraudFlagRepository;
import com.java.PayGuard.fraud.service.FraudEngineService;
import com.java.PayGuard.transaction.dto.TransferRequest;
import com.java.PayGuard.transaction.dto.TransferResponse;
import com.java.PayGuard.transaction.entity.IdempotencyKeyEntity;
import com.java.PayGuard.transaction.entity.IdempotencyState;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.transaction.service.IdempotencyService;
import com.java.PayGuard.transaction.service.TransferService;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private FraudEngineService fraudEngineService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private TransferService transferService;

    private User senderUser;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private IdempotencyKeyEntity keyEntity;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .email("sender@example.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        senderWallet = Wallet.builder()
                .id(10L)
                .user(senderUser)
                .accountNumber("ACC-SENDER")
                .balance(new BigDecimal("500.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();

        User recipientUser = User.builder()
                .id(2L)
                .email("recipient@example.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        recipientWallet = Wallet.builder()
                .id(20L)
                .user(recipientUser)
                .accountNumber("ACC-RECIPIENT")
                .balance(new BigDecimal("100.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();

        keyEntity = IdempotencyKeyEntity.builder()
                .id(100L)
                .idempotencyKey("test-key-123")
                .user(senderUser)
                .status(IdempotencyState.IN_PROGRESS)
                .build();
    }

    @Test
    @DisplayName("Should successfully execute P2P money transfer when low risk")
    void processTransfer_Success() {
        TransferRequest request = TransferRequest.builder()
                .recipientAccountNumber("ACC-RECIPIENT")
                .amount(new BigDecimal("150.0000"))
                .description("Lunch payment")
                .build();

        when(idempotencyService.acquireKey("test-key-123", senderUser)).thenReturn(keyEntity);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipientWallet));

        when(fraudEngineService.evaluateTransfer(any(), any()))
                .thenReturn(FraudEvaluationResult.builder().flagged(false).riskScore(0).build());

        when(walletRepository.findByIdWithPessimisticWriteLock(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdWithPessimisticWriteLock(20L)).thenReturn(Optional.of(recipientWallet));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<TransferResponse> responseEntity = transferService.processTransfer(request, "test-key-123", senderUser);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("COMPLETED", responseEntity.getBody().getStatus());
        assertEquals(new BigDecimal("350.0000"), senderWallet.getBalance());
        assertEquals(new BigDecimal("250.0000"), recipientWallet.getBalance());

        verify(idempotencyService, times(1)).markCompleted(eq(keyEntity), eq(200), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when sender balance is too low")
    void processTransfer_InsufficientBalance() {
        TransferRequest request = TransferRequest.builder()
                .recipientAccountNumber("ACC-RECIPIENT")
                .amount(new BigDecimal("1000.0000"))
                .build();

        when(idempotencyService.acquireKey("test-key-123", senderUser)).thenReturn(keyEntity);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipientWallet));

        when(fraudEngineService.evaluateTransfer(any(), any()))
                .thenReturn(FraudEvaluationResult.builder().flagged(false).riskScore(0).build());

        when(walletRepository.findByIdWithPessimisticWriteLock(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdWithPessimisticWriteLock(20L)).thenReturn(Optional.of(recipientWallet));

        assertThrows(InsufficientBalanceException.class,
                () -> transferService.processTransfer(request, "test-key-123", senderUser));
    }

    @Test
    @DisplayName("Should flag transaction and return 202 Accepted when fraud rule triggers")
    void processTransfer_FraudFlagged() {
        TransferRequest request = TransferRequest.builder()
                .recipientAccountNumber("ACC-RECIPIENT")
                .amount(new BigDecimal("6000.0000"))
                .build();

        when(idempotencyService.acquireKey("test-key-123", senderUser)).thenReturn(keyEntity);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipientWallet));

        when(fraudEngineService.evaluateTransfer(any(), any()))
                .thenReturn(FraudEvaluationResult.builder()
                        .flagged(true)
                        .riskScore(85)
                        .triggeredRuleCode("HIGH_AMOUNT")
                        .flagReason("Transfer amount exceeds threshold $5000")
                        .build());

        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<TransferResponse> responseEntity = transferService.processTransfer(request, "test-key-123", senderUser);

        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("FLAGGED", responseEntity.getBody().getStatus());

        // Balance should NOT be modified
        assertEquals(new BigDecimal("500.0000"), senderWallet.getBalance());

        verify(fraudFlagRepository, times(1)).save(any());
        verify(idempotencyService, times(1)).markCompleted(eq(keyEntity), eq(202), anyString());
    }
}
