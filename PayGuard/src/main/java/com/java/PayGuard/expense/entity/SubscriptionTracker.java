package com.java.PayGuard.expense.entity;

import com.java.PayGuard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName; // e.g. 'Netflix', 'AWS Cloud'

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle; // MONTHLY, YEARLY

    @Column(name = "next_renewal_date", nullable = false)
    private LocalDate nextRenewalDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, CANCEL_REQUESTED, CANCELLED

    @Column(name = "cancellation_notes", length = 255)
    private String cancellationNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
