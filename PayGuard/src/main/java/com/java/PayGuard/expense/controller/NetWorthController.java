package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.NetWorthDto;
import com.java.PayGuard.expense.service.NetWorthService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/net-worth")
@RequiredArgsConstructor
@Tag(name = "Net Worth Tracker", description = "Real-time Assets (Wallets + Savings Goals) minus Liabilities (Credit Account Expenses) balance calculator")
public class NetWorthController {

    private final NetWorthService netWorthService;

    @GetMapping
    @Operation(summary = "Calculate Net Worth", description = "Calculates total assets, total liabilities, and net worth balance for the authenticated user.")
    public ResponseEntity<NetWorthDto> getNetWorth(@AuthenticationPrincipal CustomUserDetails userDetails) {
        NetWorthDto dto = netWorthService.calculateNetWorth(userDetails.getUser());
        return ResponseEntity.ok(dto);
    }
}
