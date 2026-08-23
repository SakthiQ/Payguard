package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.ExpenseRequest;
import com.java.PayGuard.expense.dto.ExpenseResponse;
import com.java.PayGuard.expense.dto.ExpenseSummaryResponse;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import com.java.PayGuard.expense.service.ExpenseService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense Tracker", description = "Personal expense logging, search filtering, account tracking, and tax deductible reports")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Log new expense", description = "Logs an expense (Cash, Bank, Credit, or Wallet). If accountType=WALLET, automatically debits available PayGuard wallet balance.")
    public ResponseEntity<ExpenseResponse> logExpense(@Valid @RequestBody ExpenseRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        ExpenseResponse response = expenseService.logExpense(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get filtered expenses", description = "Retrieves expenses filtered by search query text, category, account type, tax deductible flag, and date range.")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(@RequestParam(value = "category", required = false) ExpenseCategory category,
                                                              @RequestParam(value = "accountType", required = false) AccountType accountType,
                                                              @RequestParam(value = "taxDeductible", required = false) Boolean taxDeductible,
                                                              @RequestParam(value = "query", required = false) String query,
                                                              @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                              @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ExpenseResponse> expenses = expenseService.getExpenses(userDetails.getUser(), category, accountType, taxDeductible, query, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get expense summary & breakdown", description = "Calculates total expenses, category breakdown map, payment method breakdown map, and tax deductible total.")
    public ResponseEntity<ExpenseSummaryResponse> getSummary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ExpenseSummaryResponse summary = expenseService.getSummary(userDetails.getUser());
        return ResponseEntity.ok(summary);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense", description = "Deletes an expense entry by ID for the authenticated owner.")
    public ResponseEntity<Void> deleteExpense(@PathVariable("id") Long id,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        expenseService.deleteExpense(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
