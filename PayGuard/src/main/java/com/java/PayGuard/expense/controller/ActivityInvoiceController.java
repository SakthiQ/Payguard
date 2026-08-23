package com.java.PayGuard.expense.controller;

import com.java.PayGuard.expense.dto.ActivityInvoiceRequest;
import com.java.PayGuard.expense.dto.ActivityInvoiceResponse;
import com.java.PayGuard.expense.service.ActivityInvoiceService;
import com.java.PayGuard.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activity-invoices")
@RequiredArgsConstructor
@Tag(name = "Activity-Linked Invoices", description = "Single-ledger activity invoices unifying income and expenses with automatic Net Profit & Profit Margin % calculations")
public class ActivityInvoiceController {

    private final ActivityInvoiceService activityInvoiceService;

    @PostMapping
    @Operation(summary = "Create Activity-Linked Invoice", description = "Creates a unified invoice linking income and multiple expense line items under a single project/activity, calculating Net Profit and Profit Margin %.")
    public ResponseEntity<ActivityInvoiceResponse> createInvoice(@Valid @RequestBody ActivityInvoiceRequest request,
                                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        ActivityInvoiceResponse response = activityInvoiceService.createInvoice(request, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List Activity Invoices", description = "Retrieves all activity invoices for the authenticated user sorted by date descending.")
    public ResponseEntity<List<ActivityInvoiceResponse>> getInvoices(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ActivityInvoiceResponse> invoices = activityInvoiceService.getInvoices(userDetails.getUser());
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Activity Invoice by ID", description = "Retrieves detailed activity invoice including all income and expense line items.")
    public ResponseEntity<ActivityInvoiceResponse> getInvoiceById(@PathVariable("id") Long id,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        ActivityInvoiceResponse invoice = activityInvoiceService.getInvoiceById(id, userDetails.getUser());
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{id}/export/csv")
    @Operation(summary = "Export Activity Invoice CSV", description = "Downloads a formatted CSV statement of an activity invoice showing line items, total income, total expenses, net profit, and profit margin.")
    public ResponseEntity<InputStreamResource> exportCsvStatement(@PathVariable("id") Long id,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        ByteArrayInputStream csvStream = activityInvoiceService.generateCsvStatement(id, userDetails.getUser());
        String filename = "Activity_Invoice_" + id + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new InputStreamResource(csvStream));
    }
}
