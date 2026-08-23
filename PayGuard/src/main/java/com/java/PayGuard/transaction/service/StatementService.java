package com.java.PayGuard.transaction.service;

import com.java.PayGuard.transaction.dto.TransactionResponse;
import com.java.PayGuard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final TransferService transferService;

    public ByteArrayInputStream generateCsvStatement(User currentUser) {
        List<TransactionResponse> transactions = transferService.getTransactionHistory(currentUser);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel compatibility
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            // Write CSV Header
            writer.println("Transaction Reference,Type,Sender Account,Recipient Account,Amount (USD),Status,Description,Created At");

            // Write Transaction Rows
            for (TransactionResponse tx : transactions) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\",\"%s\"%n",
                        escapeCsv(tx.getTransactionReference()),
                        escapeCsv(tx.getType()),
                        escapeCsv(tx.getSenderAccountNumber()),
                        escapeCsv(tx.getRecipientAccountNumber()),
                        tx.getAmount(),
                        escapeCsv(tx.getStatus()),
                        escapeCsv(tx.getDescription() != null ? tx.getDescription() : ""),
                        tx.getCreatedAt()
                );
            }
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV statement", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
