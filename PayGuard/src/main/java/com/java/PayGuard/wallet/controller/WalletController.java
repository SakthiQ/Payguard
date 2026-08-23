package com.java.PayGuard.wallet.controller;

import com.java.PayGuard.security.userdetails.CustomUserDetails;
import com.java.PayGuard.wallet.dto.DepositRequest;
import com.java.PayGuard.wallet.dto.DepositResponse;
import com.java.PayGuard.wallet.dto.WalletResponse;
import com.java.PayGuard.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Digital wallet balance management, detail retrieval, and sandbox deposits")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @Operation(summary = "Get current user wallet", description = "Returns wallet balance, account number, currency, and status for the authenticated user.")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal CustomUserDetails userDetails) {
        WalletResponse response = walletService.getWalletForCurrentUser(userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by ID", description = "Retrieves wallet details by ID (must own the wallet or be Admin).")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable("id") Long id,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        WalletResponse response = walletService.getWalletById(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get wallet balance", description = "Returns a key-value map containing currency and current available balance.")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable("id") Long id,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> response = walletService.getBalance(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deposit")
    @Operation(summary = "Deposit sandbox funds", description = "Simulates funding a wallet with sandbox funds for testing P2P transfers.")
    public ResponseEntity<DepositResponse> deposit(@PathVariable("id") Long id,
                                                    @Valid @RequestBody DepositRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        DepositResponse response = walletService.deposit(id, request, userDetails.getUser());
        return ResponseEntity.ok(response);
    }
}
