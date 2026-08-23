package com.java.PayGuard.fraud.entity;

import com.java.PayGuard.transaction.entity.Transaction;
import com.java.PayGuard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "triggered_rule_code", nullable = false, length = 50)
    private String triggeredRuleCode;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "flag_reason", nullable = false, length = 255)
    private String flagReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FlagStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id")
    private User reviewer;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
