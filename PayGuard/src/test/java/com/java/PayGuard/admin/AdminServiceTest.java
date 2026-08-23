package com.java.PayGuard.admin;

import com.java.PayGuard.admin.dto.AdminUserResponse;
import com.java.PayGuard.admin.dto.FraudRuleResponse;
import com.java.PayGuard.admin.dto.UpdateFraudRuleRequest;
import com.java.PayGuard.admin.service.AdminService;
import com.java.PayGuard.fraud.entity.FraudRule;
import com.java.PayGuard.fraud.repository.FraudRuleRepository;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.repository.UserRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
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

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User customerUser;
    private Wallet wallet;
    private FraudRule rule;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(100L)
                .email("admin@example.com")
                .role(Role.ROLE_ADMIN)
                .build();

        customerUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(Role.ROLE_CUSTOMER)
                .status(com.java.PayGuard.user.entity.UserStatus.ACTIVE)
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .user(customerUser)
                .accountNumber("ACC-1001")
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal("500.0000"))
                .build();

        rule = FraudRule.builder()
                .id(1L)
                .ruleCode("HIGH_AMOUNT")
                .ruleName("High Amount Limit")
                .thresholdValue(new BigDecimal("5000.0000"))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully freeze wallet status")
    void freezeWallet_Success() {
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        AdminUserResponse response = adminService.freezeWallet(10L, adminUser);

        assertNotNull(response);
        assertEquals("FROZEN", wallet.getStatus().name());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    @DisplayName("Should successfully unfreeze wallet status")
    void unfreezeWallet_Success() {
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        AdminUserResponse response = adminService.unfreezeWallet(10L, adminUser);

        assertNotNull(response);
        assertEquals("ACTIVE", wallet.getStatus().name());
        verify(walletRepository, times(1)).save(wallet);
    }

    @Test
    @DisplayName("Should update fraud rule threshold and return FraudRuleResponse DTO")
    void updateFraudRule_Success() {
        UpdateFraudRuleRequest request = UpdateFraudRuleRequest.builder()
                .thresholdValue(new BigDecimal("10000.0000"))
                .timeWindowSeconds(600)
                .enabled(true)
                .build();

        when(fraudRuleRepository.findByRuleCode("HIGH_AMOUNT")).thenReturn(Optional.of(rule));
        when(fraudRuleRepository.save(any(FraudRule.class))).thenAnswer(inv -> inv.getArgument(0));

        FraudRuleResponse response = adminService.updateFraudRule("HIGH_AMOUNT", request, adminUser);

        assertNotNull(response);
        assertEquals("HIGH_AMOUNT", response.getRuleCode());
        assertEquals(new BigDecimal("10000.0000"), response.getThresholdValue());
        assertEquals(600, response.getTimeWindowSeconds());
    }
}
