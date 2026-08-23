package com.java.PayGuard.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String userStatus;
    private Long walletId;
    private String accountNumber;
    private BigDecimal balance;
    private String walletStatus;
    private LocalDateTime createdAt;
}
