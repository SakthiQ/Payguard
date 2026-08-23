package com.java.PayGuard.expense.entity;

import com.java.PayGuard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "category"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "monthly_cap", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyCap;

    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;

    @Column(name = "rollover_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rolloverAmount;

    @Column(name = "warning_threshold_percent", nullable = false)
    private Integer warningThresholdPercent; // Default 80

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
