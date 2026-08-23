package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.SubscriptionDto;
import com.java.PayGuard.expense.service.SubscriptionService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Hub", description = "SaaS subscription tracking, renewal calendar countdown lookahead, and 1-click cancellation requests")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Add recurring subscription", description = "Tracks a SaaS subscription with billing cycle and next renewal date.")
    public ResponseEntity<SubscriptionDto> addSubscription(@RequestParam("serviceName") String serviceName,
                                                             @RequestParam("amount") BigDecimal amount,
                                                             @RequestParam(value = "billingCycle", required = false) String billingCycle,
                                                             @RequestParam("nextRenewalDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextRenewalDate,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionDto sub = subscriptionService.addSubscription(userDetails.getUser(), serviceName, amount, billingCycle, nextRenewalDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(sub);
    }

    @GetMapping
    @Operation(summary = "Get subscriptions", description = "Retrieves user's active and pending-cancellation subscriptions sorted by renewal date lookahead.")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SubscriptionDto> subs = subscriptionService.getSubscriptions(userDetails.getUser());
        return ResponseEntity.ok(subs);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Initiate 1-Click Cancellation Request", description = "Flags a subscription for cancellation with cancellation guidance notes.")
    public ResponseEntity<SubscriptionDto> requestCancellation(@PathVariable("id") Long id,
                                                                @RequestParam(value = "notes", required = false) String notes,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionDto cancelled = subscriptionService.requestCancellation(id, userDetails.getUser(), notes);
        return ResponseEntity.ok(cancelled);
    }
}
