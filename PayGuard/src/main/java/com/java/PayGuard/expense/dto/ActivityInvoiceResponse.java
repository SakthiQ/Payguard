package com.java.PayGuard.expense.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityInvoiceResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String activityTitle;
    private String clientName;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private BigDecimal profitMarginPercent;
    private String status;
    private List<InvoiceLineItemDto> lineItems;
    private LocalDateTime createdAt;
}
