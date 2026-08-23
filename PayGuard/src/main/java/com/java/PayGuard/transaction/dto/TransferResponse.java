package com.java.PayGuard.transaction.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private String transactionReference;
    private String senderAccountNumber;
    private String recipientAccountNumber;
    private BigDecimal amount;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
