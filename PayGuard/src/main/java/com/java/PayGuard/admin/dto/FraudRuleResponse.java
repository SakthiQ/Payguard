package com.java.PayGuard.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleResponse {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private BigDecimal thresholdValue;
    private Integer timeWindowSeconds;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
