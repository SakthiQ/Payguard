package com.java.PayGuard.fraud.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudEvaluationResult {
    private boolean flagged;
    private int riskScore;
    private String triggeredRuleCode;
    private String flagReason;
}
