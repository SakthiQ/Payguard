package com.java.PayGuard.auth.service;

import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.auth.dto.AuthResponse;
import com.java.PayGuard.auth.dto.LoginRequest;
import com.java.PayGuard.auth.dto.RegisterRequest;
import com.java.PayGuard.auth.dto.UserResponse;
import com.java.PayGuard.common.exception.AuthenticationException;
import com.java.PayGuard.common.exception.DuplicateResourceException;
import com.java.PayGuard.security.util.JwtTokenProvider;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.entity.UserStatus;
import com.java.PayGuard.user.repository.UserRepository;
import com.java.PayGuard.wallet.entity.Wallet;
import com.java.PayGuard.wallet.entity.WalletStatus;
import com.java.PayGuard.wallet.repository.WalletRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt for already-existing email: {}", request.getEmail());
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-provision primary Wallet for customer
        String accountNumber = generateAccountNumber();
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO.setScale(4))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);

        log.info("New customer registered: userId={}, email={}, accountNumber={}",
                savedUser.getId(), savedUser.getEmail(), accountNumber);

        // Emit audit events after successful registration
        publishAuditEvent(savedUser, "USER_REGISTERED", savedUser.getId().toString(), Map.of(
                "email", savedUser.getEmail(),
                "role", savedUser.getRole().name()
        ));
        publishAuditEvent(savedUser, "WALLET_CREATED", accountNumber, Map.of(
                "accountNumber", accountNumber,
                "currency", "USD"
        ));

        return UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .status(savedUser.getStatus().name())
                .accountNumber(accountNumber)
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("User credentials not found."));

            ResponseCookie jwtCookie = jwtTokenProvider.generateJwtCookie(user.getEmail(), user.getRole().name());
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

            log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());

            return AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .message("Login successful.")
                    .build();
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password.");
        }
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie cleanCookie = jwtTokenProvider.getCleanJwtCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());
        log.info("User session cleared (logout)");
    }

    private String generateAccountNumber() {
        long number = 1000000000L + (long) (RANDOM.nextDouble() * 9000000000L);
        return "ACC-" + number;
    }

    private void publishAuditEvent(User actor, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(actor.getId())
                .actorEmail(actor.getEmail())
                .actorRole(actor.getRole().name())
                .resourceType("AUTH")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }
}
