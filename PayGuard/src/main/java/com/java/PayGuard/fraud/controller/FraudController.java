package com.java.PayGuard.fraud.controller;

import com.java.PayGuard.fraud.dto.FraudFlagResponse;
import com.java.PayGuard.fraud.dto.FraudReviewRequest;
import com.java.PayGuard.fraud.service.FraudReviewService;
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
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FRAUD_ANALYST', 'ADMIN')")
@Tag(name = "Fraud Analyst Queue", description = "Risk flag inspection and decision review workflow (Analyst / Admin)")
public class FraudController {

    private final FraudReviewService fraudReviewService;

    @GetMapping("/flags")
    @Operation(summary = "Get pending fraud review queue", description = "Retrieves all transactions currently in UNDER_REVIEW status requiring analyst decision.")
    public ResponseEntity<List<FraudFlagResponse>> getPendingFlags() {
        List<FraudFlagResponse> flags = fraudReviewService.getPendingFlags();
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/flags/{id}")
    @Operation(summary = "Get fraud flag details by ID", description = "Retrieves risk score, triggered rule code, and flag reason for a specific flag.")
    public ResponseEntity<FraudFlagResponse> getFlagById(@PathVariable("id") Long id) {
        FraudFlagResponse flag = fraudReviewService.getFlagById(id);
        return ResponseEntity.ok(flag);
    }

    @PutMapping("/flags/{id}/review")
    @Operation(summary = "Submit analyst review decision", description = "Executes APPROVE or DECLINE action on a flagged transaction with mandatory investigation notes.")
    public ResponseEntity<FraudFlagResponse> reviewFlag(@PathVariable("id") Long id,
                                                         @Valid @RequestBody FraudReviewRequest request,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        FraudFlagResponse response = fraudReviewService.reviewFlag(id, request, userDetails.getUser());
        return ResponseEntity.ok(response);
    }
}
