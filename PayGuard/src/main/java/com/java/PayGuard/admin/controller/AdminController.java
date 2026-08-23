package com.java.PayGuard.admin.controller;

import com.java.PayGuard.admin.dto.AdminUserResponse;
import com.java.PayGuard.admin.dto.FraudRuleResponse;
import com.java.PayGuard.admin.dto.UpdateFraudRuleRequest;
import com.java.PayGuard.admin.service.AdminService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Command Center", description = "User management, wallet freeze/unfreeze controls, and fraud rule engine configuration (Admin only)")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users & wallets", description = "Lists all system users alongside their associated wallet statuses, account numbers, and balances.")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/wallets/{id}/freeze")
    @Operation(summary = "Freeze wallet", description = "Changes wallet status to FROZEN, preventing outgoing transfers.")
    public ResponseEntity<AdminUserResponse> freezeWallet(@PathVariable("id") Long walletId,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminUserResponse response = adminService.freezeWallet(walletId, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/wallets/{id}/unfreeze")
    @Operation(summary = "Unfreeze wallet", description = "Restores wallet status to ACTIVE, enabling transfers.")
    public ResponseEntity<AdminUserResponse> unfreezeWallet(@PathVariable("id") Long walletId,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminUserResponse response = adminService.unfreezeWallet(walletId, userDetails.getUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fraud-rules")
    @Operation(summary = "Get all fraud rules", description = "Lists all fraud detection rules, threshold values, time windows, and enabled states.")
    public ResponseEntity<List<FraudRuleResponse>> getAllFraudRules() {
        List<FraudRuleResponse> rules = adminService.getAllFraudRules();
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/fraud-rules/{ruleCode}")
    @Operation(summary = "Update fraud rule threshold", description = "Modifies threshold value, time window, or enabled state for a specific rule (e.g. HIGH_AMOUNT, HIGH_VELOCITY).")
    public ResponseEntity<FraudRuleResponse> updateFraudRule(@PathVariable("ruleCode") String ruleCode,
                                                              @Valid @RequestBody UpdateFraudRuleRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        FraudRuleResponse updatedRule = adminService.updateFraudRule(ruleCode, request, userDetails.getUser());
        return ResponseEntity.ok(updatedRule);
    }
}
