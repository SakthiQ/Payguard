package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.ExpenseRequest;
import com.java.PayGuard.expense.service.ExpenseAiInsightService;
import com.java.PayGuard.expense.service.ReceiptOcrService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expense-analytics")
@RequiredArgsConstructor
@Tag(name = "Expense AI & OCR Analytics", description = "AI-powered spending pattern insights, subscription audits, and OCR receipt image scanner")
public class ExpenseAnalyticsController {

    private final ExpenseAiInsightService expenseAiInsightService;
    private final ReceiptOcrService receiptOcrService;

    @GetMapping("/insights")
    @Operation(summary = "Get AI Spending Insights", description = "Generates AI spending insights, dominance metrics, subscription optimization advice, and tax alerts.")
    public ResponseEntity<List<String>> getInsights(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<String> insights = expenseAiInsightService.generateSpendingInsights(userDetails.getUser());
        return ResponseEntity.ok(insights);
    }

    @PostMapping("/scan-receipt")
    @Operation(summary = "Scan Receipt Image (OCR)", description = "Processes a receipt image file name and extracts merchant name, amount, category, and tax deductible flag.")
    public ResponseEntity<ExpenseRequest> scanReceipt(@RequestParam("filename") String filename) {
        ExpenseRequest request = receiptOcrService.scanReceiptImage(filename);
        return ResponseEntity.ok(request);
    }
}
