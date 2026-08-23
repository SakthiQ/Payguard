package com.java.PayGuard.audit.controller;

import com.java.PayGuard.audit.document.AuditLogDocument;
import com.java.PayGuard.audit.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FRAUD_ANALYST')")
@Tag(name = "Audit Stream", description = "Asynchronous MongoDB audit trail query endpoints (Admin / Analyst)")
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/logs")
    @Operation(summary = "Get all audit logs", description = "Retrieves all asynchronous MongoDB domain audit log documents sorted by timestamp descending.")
    public ResponseEntity<List<AuditLogDocument>> getAllAuditLogs() {
        List<AuditLogDocument> logs = auditQueryService.getAllAuditLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get audit logs by user ID", description = "Filters MongoDB audit documents where actorUserId matches the specified user ID.")
    public ResponseEntity<List<AuditLogDocument>> getAuditLogsByUserId(@PathVariable("userId") Long userId) {
        List<AuditLogDocument> logs = auditQueryService.getAuditLogsByUserId(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/event/{eventType}")
    @Operation(summary = "Get audit logs by event type", description = "Filters audit documents by event type (e.g. USER_REGISTERED, WALLET_TOPUP, TRANSFER_COMPLETED, FRAUD_APPROVED).")
    public ResponseEntity<List<AuditLogDocument>> getAuditLogsByEventType(@PathVariable("eventType") String eventType) {
        List<AuditLogDocument> logs = auditQueryService.getAuditLogsByEventType(eventType);
        return ResponseEntity.ok(logs);
    }
}
