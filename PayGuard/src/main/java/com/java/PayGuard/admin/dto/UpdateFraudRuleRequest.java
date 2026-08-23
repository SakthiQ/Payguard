package com.java.PayGuard.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFraudRuleRequest {

    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.0001", message = "Threshold value must be greater than zero")
    private BigDecimal thresholdValue;

    private Integer timeWindowSeconds;

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
