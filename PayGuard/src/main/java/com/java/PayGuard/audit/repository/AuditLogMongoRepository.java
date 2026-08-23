package com.java.PayGuard.audit.repository;

import com.java.PayGuard.audit.document.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {
    List<AuditLogDocument> findByActorUserId(Long actorUserId);
    List<AuditLogDocument> findByEventType(String eventType);
}
