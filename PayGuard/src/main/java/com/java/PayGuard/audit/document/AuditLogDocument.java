package com.java.PayGuard.audit.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDocument {

    @Id
    private String id;

    @Field("event_id")
    private String eventId;

    @Field("event_type")
    private String eventType;

    @Field("actor_user_id")
    private Long actorUserId;

    @Field("actor_email")
    private String actorEmail;

    @Field("actor_role")
    private String actorRole;

    @Field("resource_type")
    private String resourceType;

    @Field("resource_id")
    private String resourceId;

    @Field("payload")
    private Map<String, Object> payload;

    @Field("timestamp")
    private Instant timestamp;
}
