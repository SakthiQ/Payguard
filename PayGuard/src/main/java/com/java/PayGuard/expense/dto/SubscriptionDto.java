package com.java.PayGuard.expense.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDto {
    private Long id;
    private String serviceName;
    private BigDecimal amount;
    private String billingCycle;
    private LocalDate nextRenewalDate;
    private Long daysUntilRenewal;
    private String status;
    private String cancellationNotes;
}
