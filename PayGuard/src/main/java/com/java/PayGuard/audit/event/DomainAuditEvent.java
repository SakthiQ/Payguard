package com.java.PayGuard.audit.event;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainAuditEvent {
    private String eventId;
    private String eventType;
    private Long actorUserId;
    private String actorEmail;
    private String actorRole;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> payload;
    private Instant timestamp;
}
