package com.java.PayGuard.audit.listener;

import com.java.PayGuard.audit.document.AuditLogDocument;
import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.audit.repository.AuditLogMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogMongoRepository auditLogMongoRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainAuditEvent(DomainAuditEvent event) {
        try {
            AuditLogDocument doc = AuditLogDocument.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .actorUserId(event.getActorUserId())
                    .actorEmail(event.getActorEmail())
                    .actorRole(event.getActorRole())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .payload(event.getPayload())
                    .timestamp(event.getTimestamp())
                    .build();

            auditLogMongoRepository.save(doc);
            log.debug("Persisted async audit log event: {}", event.getEventType());
        } catch (Exception ex) {
            log.error("Failed to persist MongoDB audit event for eventId: {}", event.getEventId(), ex);
        }
    }
}
