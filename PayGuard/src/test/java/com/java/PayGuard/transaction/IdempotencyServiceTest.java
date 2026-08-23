package com.java.PayGuard.transaction;

import com.java.PayGuard.common.exception.DuplicateTransactionException;
import com.java.PayGuard.transaction.entity.IdempotencyKeyEntity;
import com.java.PayGuard.transaction.entity.IdempotencyState;
import com.java.PayGuard.transaction.repository.IdempotencyKeyRepository;
import com.java.PayGuard.transaction.service.IdempotencyService;
import com.java.PayGuard.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private User user;
    private IdempotencyKeyEntity keyEntity;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").build();
        keyEntity = IdempotencyKeyEntity.builder()
                .id(10L)
                .idempotencyKey("KEY-123")
                .user(user)
                .status(IdempotencyState.IN_PROGRESS)
                .build();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when Idempotency-Key header is missing or blank")
    void acquireKey_MissingHeader() {
        assertThrows(IllegalArgumentException.class, () -> idempotencyService.acquireKey("", user));
        assertThrows(IllegalArgumentException.class, () -> idempotencyService.acquireKey(null, user));
    }

    @Test
    @DisplayName("Should throw DuplicateTransactionException when key is currently IN_PROGRESS")
    void acquireKey_InProgress() {
        when(idempotencyKeyRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(keyEntity));

        assertThrows(DuplicateTransactionException.class, () -> idempotencyService.acquireKey("KEY-123", user));
    }

    @Test
    @DisplayName("Should return existing entity when status is COMPLETED")
    void acquireKey_Completed() {
        keyEntity.setStatus(IdempotencyState.COMPLETED);
        keyEntity.setResponseBody("{\"status\":\"COMPLETED\"}");
        when(idempotencyKeyRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(keyEntity));

        IdempotencyKeyEntity result = idempotencyService.acquireKey("KEY-123", user);

        assertNotNull(result);
        assertEquals(IdempotencyState.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("Should catch concurrent DataIntegrityViolationException and throw DuplicateTransactionException")
    void acquireKey_ConcurrentConstraintViolation() {
        when(idempotencyKeyRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate key entry"));

        assertThrows(DuplicateTransactionException.class, () -> idempotencyService.acquireKey("KEY-123", user));
    }
}
