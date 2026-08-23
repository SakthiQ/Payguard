package com.java.PayGuard.expense.entity;

import com.java.PayGuard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activity_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_title", nullable = false, length = 150)
    private String activityTitle; // e.g. 'NYC Tech Conference 2026', 'Client Web Redesign'

    @Column(name = "client_name", length = 100)
    private String clientName;

    @Column(name = "total_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalIncome;

    @Column(name = "total_expenses", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalExpenses;

    @Column(name = "net_profit", nullable = false, precision = 19, scale = 4)
    private BigDecimal netProfit;

    @Column(name = "profit_margin_percent", nullable = false, precision = 10, scale = 2)
    private BigDecimal profitMarginPercent;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // DRAFT, FINALIZED, PAID

    @OneToMany(mappedBy = "activityInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
