package com.java.PayGuard.expense.entity;

import com.java.PayGuard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "tags", length = 255)
    private String tags; // e.g. "#vacation2026,#client_alpha"

    @Column(name = "tax_deductible", nullable = false)
    private Boolean taxDeductible;

    @Column(name = "recurring", nullable = false)
    private Boolean recurring;

    @Column(name = "activity_invoice_id")
    private Long activityInvoiceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
