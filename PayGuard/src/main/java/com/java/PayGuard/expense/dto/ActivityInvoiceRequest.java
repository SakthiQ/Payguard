package com.java.PayGuard.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityInvoiceRequest {

    @NotBlank(message = "Activity title is required")
    private String activityTitle; // e.g. 'NYC Tech Conference 2026'

    private String clientName;

    private String status; // DRAFT, FINALIZED, PAID

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceLineItemDto> lineItems;
}
