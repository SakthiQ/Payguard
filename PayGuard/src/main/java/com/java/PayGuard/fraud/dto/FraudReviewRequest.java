package com.java.PayGuard.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudReviewRequest {

    @NotBlank(message = "Action is required (APPROVE or DECLINE)")
    private String action;

    @NotBlank(message = "Review notes are mandatory for investigation accountability")
    private String notes;
}
