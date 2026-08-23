package com.java.PayGuard.transaction.service;

import com.java.PayGuard.common.exception.DuplicateTransactionException;

import com.java.PayGuard.transaction.entity.IdempotencyKeyEntity;
import com.java.PayGuard.transaction.entity.IdempotencyState;
import com.java.PayGuard.transaction.repository.IdempotencyKeyRepository;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKeyEntity acquireKey(String keyHeader, User user) {
        if (keyHeader == null || keyHeader.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key HTTP header is mandatory for payment transactions.");
        }

        Optional<IdempotencyKeyEntity> existingOpt = idempotencyKeyRepository.findByIdempotencyKey(keyHeader);
        if (existingOpt.isPresent()) {
            IdempotencyKeyEntity existing = existingOpt.get();
            if (existing.getStatus() == IdempotencyState.IN_PROGRESS) {
                log.warn("Duplicate request detected for key {} currently IN_PROGRESS", keyHeader);
                throw new DuplicateTransactionException("A transfer transaction with this Idempotency-Key is currently in progress.");
            }
            return existing;
        }

        try {
            IdempotencyKeyEntity keyEntity = IdempotencyKeyEntity.builder()
                    .idempotencyKey(keyHeader)
                    .user(user)
                    .status(IdempotencyState.IN_PROGRESS)
                    .build();
            return idempotencyKeyRepository.saveAndFlush(keyEntity);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate key insertion caught for key {}", keyHeader);
            throw new DuplicateTransactionException("A transfer transaction with this Idempotency-Key is currently in progress.");
        }
    }

    @Transactional
    public void markCompleted(IdempotencyKeyEntity keyEntity, int responseCode, String responseBodyJson) {
        keyEntity.setStatus(IdempotencyState.COMPLETED);
        keyEntity.setResponseCode(responseCode);
        keyEntity.setResponseBody(responseBodyJson);
        idempotencyKeyRepository.save(keyEntity);
    }
}
