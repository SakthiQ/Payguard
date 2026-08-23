package com.java.PayGuard.expense.service;

import com.java.PayGuard.expense.dto.ExpenseRequest;
import com.java.PayGuard.expense.entity.AccountType;
import com.java.PayGuard.expense.entity.ExpenseCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class ReceiptOcrService {

    private final Random random = new Random();

    public ExpenseRequest scanReceiptImage(String filename) {
        // Simulated OCR extraction from receipt image filename/content
        String cleanName = filename != null ? filename.toLowerCase() : "";

        ExpenseCategory category = ExpenseCategory.FOOD;
        String merchant = "Starbucks Coffee";
        BigDecimal amount = new BigDecimal("14.50");

        if (cleanName.contains("uber") || cleanName.contains("taxi") || cleanName.contains("flight")) {
            category = ExpenseCategory.TRAVEL;
            merchant = "Uber Technologies";
            amount = new BigDecimal("32.80");
        } else if (cleanName.contains("aws") || cleanName.contains("cloud") || cleanName.contains("saas")) {
            category = ExpenseCategory.BUSINESS;
            merchant = "AWS Cloud Services";
            amount = new BigDecimal("120.00");
        } else if (cleanName.contains("grocery") || cleanName.contains("walmart") || cleanName.contains("target")) {
            category = ExpenseCategory.FOOD;
            merchant = "Whole Foods Market";
            amount = new BigDecimal("87.40");
        }

        return ExpenseRequest.builder()
                .category(category)
                .accountType(AccountType.CASH)
                .amount(amount)
                .merchantName(merchant)
                .description("Auto-scanned receipt: " + filename)
                .tags("#receipt_ocr")
                .taxDeductible(category == ExpenseCategory.BUSINESS)
                .recurring(false)
                .build();
    }
}
