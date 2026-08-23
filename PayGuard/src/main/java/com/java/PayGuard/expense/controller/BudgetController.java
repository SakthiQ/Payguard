package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.BudgetProgressDto;
import com.java.PayGuard.expense.entity.BudgetLimit;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.service.BudgetService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets & Caps", description = "Monthly category spending cap management, 80%/100% threshold alert monitoring, and rollover allowances")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Set category monthly budget cap", description = "Configures a spending limit for a specific category (e.g. FOOD, ENTERTAINMENT).")
    public ResponseEntity<BudgetLimit> setBudget(@RequestParam("category") ExpenseCategory category,
                                                 @RequestParam("monthlyCap") BigDecimal monthlyCap,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        BudgetLimit limit = budgetService.setBudget(userDetails.getUser(), category, monthlyCap);
        return ResponseEntity.ok(limit);
    }

    @GetMapping("/progress")
    @Operation(summary = "Get budget progress & alerts", description = "Retrieves spending progress for all configured categories with status pills (OK, WARNING_80, OVERSPENT_100) and rollover balances.")
    public ResponseEntity<List<BudgetProgressDto>> getBudgetProgress(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<BudgetProgressDto> progress = budgetService.getBudgetProgress(userDetails.getUser());
        return ResponseEntity.ok(progress);
    }
}
