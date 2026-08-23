package com.java.PayGuard.fraud.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudFlagResponse {
    private Long flagId;
    private Long transactionId;
    private String transactionReference;
    private String senderAccountNumber;
    private String recipientAccountNumber;
    private BigDecimal amount;
    private String triggeredRuleCode;
    private Integer riskScore;
    private String flagReason;
    private String status;
    private String reviewerEmail;
    private String reviewNotes;
    private String aiRiskInsight;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
