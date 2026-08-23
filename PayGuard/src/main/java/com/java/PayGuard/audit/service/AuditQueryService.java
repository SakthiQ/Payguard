package com.java.PayGuard.audit.service;

import com.java.PayGuard.audit.document.AuditLogDocument;
import com.java.PayGuard.audit.repository.AuditLogMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogMongoRepository auditLogMongoRepository;

    public List<AuditLogDocument> getAllAuditLogs() {
        return auditLogMongoRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public List<AuditLogDocument> getAuditLogsByUserId(Long userId) {
        return auditLogMongoRepository.findByActorUserId(userId);
    }

    public List<AuditLogDocument> getAuditLogsByEventType(String eventType) {
        return auditLogMongoRepository.findByEventType(eventType);
    }
}
