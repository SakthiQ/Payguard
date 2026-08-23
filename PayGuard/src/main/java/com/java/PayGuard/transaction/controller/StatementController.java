package com.java.PayGuard.transaction.controller;

import com.java.PayGuard.security.userdetails.CustomUserDetails;
import com.java.PayGuard.transaction.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Statements", description = "Account statement generation and CSV report exports")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/export/csv")
    @Operation(summary = "Export CSV Account Statement", description = "Generates and downloads a CSV statement containing all transaction activity for the authenticated user.")
    public ResponseEntity<InputStreamResource> exportCsvStatement(@AuthenticationPrincipal CustomUserDetails userDetails) {
        ByteArrayInputStream csvStream = statementService.generateCsvStatement(userDetails.getUser());
        String filename = "PayGuard_Statement_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new InputStreamResource(csvStream));
    }
}
