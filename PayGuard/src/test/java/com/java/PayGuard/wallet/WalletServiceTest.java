package com.java.PayGuard.wallet;

import com.java.PayGuard.common.exception.AuthorizationException;
import com.java.PayGuard.transaction.repository.TransactionRepository;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.entity.UserStatus;
import com.java.PayGuard.wallet.dto.DepositRequest;
import com.java.PayGuard.wallet.dto.DepositResponse;
import com.java.PayGuard.wallet.dto.WalletResponse;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import com.java.PayGuard.wallet.service.WalletService;
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

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalletService walletService;

    private User owner;
    private User otherUser;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@example.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .user(owner)
                .accountNumber("ACC-1001")
                .balance(new BigDecimal("100.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should successfully return wallet details for owner")
    void getWalletById_OwnerSuccess() {
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWalletById(10L, owner);

        assertNotNull(response);
        assertEquals("ACC-1001", response.getAccountNumber());
        assertEquals(new BigDecimal("100.0000"), response.getBalance());
    }

    @Test
    @DisplayName("Should throw AuthorizationException when non-owner non-admin accesses wallet")
    void getWalletById_Unauthorized() {
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        assertThrows(AuthorizationException.class, () -> walletService.getWalletById(10L, otherUser));
    }

    @Test
    @DisplayName("Should successfully deposit sandbox funds into wallet")
    void deposit_Success() {
        DepositRequest request = DepositRequest.builder()
                .amount(new BigDecimal("50.0000"))
                .build();

        when(walletRepository.findByIdWithPessimisticWriteLock(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositResponse response = walletService.deposit(10L, request, owner);

        assertNotNull(response);
        assertEquals(new BigDecimal("150.0000"), response.getNewBalance());
        assertTrue(response.getTransactionReference().startsWith("DEP-"));

        verify(walletRepository, times(1)).save(any(Wallet.class));
        verify(transactionRepository, times(1)).save(any());
        verify(eventPublisher, times(1)).publishEvent((Object) any());
    }
}
