package com.java.PayGuard.expense.service;

import com.java.PayGuard.audit.event.DomainAuditEvent;
import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.expense.dto.ActivityInvoiceRequest;
import com.java.PayGuard.expense.dto.ActivityInvoiceResponse;
import com.java.PayGuard.expense.dto.InvoiceLineItemDto;
import com.java.PayGuard.expense.entity.ActivityInvoice;
import com.java.PayGuard.expense.entity.InvoiceLineItem;
import com.java.PayGuard.expense.repository.ActivityInvoiceRepository;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityInvoiceService {

    private final ActivityInvoiceRepository activityInvoiceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ActivityInvoiceResponse createInvoice(ActivityInvoiceRequest request, User currentUser) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        List<InvoiceLineItem> lineItems = new ArrayList<>();

        ActivityInvoice invoice = ActivityInvoice.builder()
                .user(currentUser)
                .activityTitle(request.getActivityTitle().trim())
                .clientName(request.getClientName() != null ? request.getClientName().trim() : "")
                .status(request.getStatus() != null ? request.getStatus().toUpperCase() : "FINALIZED")
                .totalIncome(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .netProfit(BigDecimal.ZERO)
                .profitMarginPercent(BigDecimal.ZERO)
                .build();

        for (InvoiceLineItemDto dto : request.getLineItems()) {
            BigDecimal amt = dto.getAmount().setScale(4, RoundingMode.HALF_EVEN);
            String type = dto.getType().trim().toUpperCase();

            if ("INCOME".equals(type)) {
                totalIncome = totalIncome.add(amt);
            } else if ("EXPENSE".equals(type)) {
                totalExpenses = totalExpenses.add(amt);
            } else {
                throw new IllegalArgumentException("Line item type must be INCOME or EXPENSE");
            }

            InvoiceLineItem item = InvoiceLineItem.builder()
                    .activityInvoice(invoice)
                    .type(type)
                    .description(dto.getDescription().trim())
                    .amount(amt)
                    .category(dto.getCategory() != null ? dto.getCategory().trim().toUpperCase() : "GENERAL")
                    .build();

            lineItems.add(item);
        }

        BigDecimal netProfit = totalIncome.subtract(totalExpenses).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal marginPercent = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            marginPercent = netProfit.multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_EVEN);
        }

        invoice.setTotalIncome(totalIncome);
        invoice.setTotalExpenses(totalExpenses);
        invoice.setNetProfit(netProfit);
        invoice.setProfitMarginPercent(marginPercent);
        invoice.setLineItems(lineItems);

        ActivityInvoice saved = activityInvoiceRepository.save(invoice);

        publishAuditEvent(currentUser, "ACTIVITY_INVOICE_CREATED", saved.getId().toString(), Map.of(
                "activityTitle", saved.getActivityTitle(),
                "totalIncome", totalIncome.toString(),
                "totalExpenses", totalExpenses.toString(),
                "netProfit", netProfit.toString(),
                "marginPercent", marginPercent.toString()
        ));

        log.info("Created ActivityInvoice #{} '{}' for user {}: Net Profit = ${} ({}% margin)",
                saved.getId(), saved.getActivityTitle(), currentUser.getEmail(), netProfit, marginPercent);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActivityInvoiceResponse> getInvoices(User currentUser) {
        return activityInvoiceRepository.findAllByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActivityInvoiceResponse getInvoiceById(Long id, User currentUser) {
        ActivityInvoice invoice = activityInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity invoice not found with ID: " + id));

        if (!invoice.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to view this activity invoice.");
        }

        return mapToResponse(invoice);
    }

    public ByteArrayInputStream generateCsvStatement(Long id, User currentUser) {
        ActivityInvoiceResponse invoice = getInvoiceById(id, currentUser);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // UTF-8 BOM
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.printf("ACTIVITY INVOICE STATEMENT - %s%n", escapeCsv(invoice.getActivityTitle()));
            writer.printf("Client: %s%n", escapeCsv(invoice.getClientName()));
            writer.printf("Status: %s%n", invoice.getStatus());
            writer.printf("Created At: %s%n%n", invoice.getCreatedAt());

            writer.println("Line Item ID,Type,Category,Description,Amount (USD)");

            for (InvoiceLineItemDto item : invoice.getLineItems()) {
                writer.printf("\"%d\",\"%s\",\"%s\",\"%s\",%.2f%n",
                        item.getId(),
                        escapeCsv(item.getType()),
                        escapeCsv(item.getCategory()),
                        escapeCsv(item.getDescription()),
                        item.getAmount()
                );
            }

            writer.println();
            writer.printf("TOTAL INCOME,%.2f%n", invoice.getTotalIncome());
            writer.printf("TOTAL EXPENSES,%.2f%n", invoice.getTotalExpenses());
            writer.printf("NET PROFIT,%.2f%n", invoice.getNetProfit());
            writer.printf("PROFIT MARGIN,%.2f%%%n", invoice.getProfitMarginPercent());

            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Activity Invoice CSV", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void publishAuditEvent(User user, String eventType, String resourceId, Map<String, Object> payload) {
        DomainAuditEvent auditEvent = DomainAuditEvent.builder()
                .eventId("EVT-" + UUID.randomUUID())
                .eventType(eventType)
                .actorUserId(user.getId())
                .actorEmail(user.getEmail())
                .actorRole(user.getRole().name())
                .resourceType("ACTIVITY_INVOICE")
                .resourceId(resourceId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishEvent(auditEvent);
    }

    private ActivityInvoiceResponse mapToResponse(ActivityInvoice invoice) {
        List<InvoiceLineItemDto> lineDtos = invoice.getLineItems().stream()
                .map(item -> InvoiceLineItemDto.builder()
                        .id(item.getId())
                        .type(item.getType())
                        .description(item.getDescription())
                        .amount(item.getAmount())
                        .category(item.getCategory())
                        .build())
                .collect(Collectors.toList());

        return ActivityInvoiceResponse.builder()
                .id(invoice.getId())
                .userId(invoice.getUser().getId())
                .userEmail(invoice.getUser().getEmail())
                .activityTitle(invoice.getActivityTitle())
                .clientName(invoice.getClientName())
                .totalIncome(invoice.getTotalIncome())
                .totalExpenses(invoice.getTotalExpenses())
                .netProfit(invoice.getNetProfit())
                .profitMarginPercent(invoice.getProfitMarginPercent())
                .status(invoice.getStatus())
                .lineItems(lineDtos)
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
