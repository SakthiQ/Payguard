package com.java.PayGuard.transaction.controller;

import com.java.PayGuard.security.userdetails.CustomUserDetails;
import com.java.PayGuard.transaction.dto.TransactionResponse;
import com.java.PayGuard.transaction.dto.TransferRequest;
import com.java.PayGuard.transaction.dto.TransferResponse;
import com.java.PayGuard.transaction.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Peer-to-peer idempotent money transfers and transaction history")
public class TransactionController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Execute P2P Transfer", description = "Executes an idempotent money transfer using the Idempotency-Key header. Returns 200 COMPLETED, 202 FLAGGED (for fraud review), or 400/409 errors.")
    public ResponseEntity<TransferResponse> createTransfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                            @Valid @RequestBody TransferRequest request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return transferService.processTransfer(request, idempotencyKey, userDetails.getUser());
    }

    @GetMapping
    @Operation(summary = "Get transaction history", description = "Retrieves all deposits and transfers associated with the authenticated user's wallet.")
    public ResponseEntity<List<TransactionResponse>> getHistory(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TransactionResponse> history = transferService.getTransactionHistory(userDetails.getUser());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieves specific transaction details by ID for authenticated owner or admin.")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable("id") Long id,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        TransactionResponse response = transferService.getTransactionById(id, userDetails.getUser());
        return ResponseEntity.ok(response);
    }
}
