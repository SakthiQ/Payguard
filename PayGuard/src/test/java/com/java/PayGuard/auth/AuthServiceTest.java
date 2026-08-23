package com.java.PayGuard.auth;

import com.java.PayGuard.auth.dto.RegisterRequest;
import com.java.PayGuard.auth.dto.UserResponse;
import com.java.PayGuard.auth.service.AuthService;
import com.java.PayGuard.common.exception.DuplicateResourceException;
import com.java.PayGuard.security.util.JwtTokenProvider;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.repository.UserRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    @DisplayName("Should successfully register user and auto-provision primary wallet")
    void register_Success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        
        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .firstName("John")
                .lastName("Doe")
                .role(com.java.PayGuard.user.entity.Role.ROLE_CUSTOMER)
                .status(com.java.PayGuard.user.entity.UserStatus.ACTIVE)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("ROLE_CUSTOMER", response.getRole());
        assertNotNull(response.getAccountNumber());
        assertTrue(response.getAccountNumber().startsWith("ACC-"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void register_DuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}
