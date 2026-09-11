package com.java.PayGuard.expense;

import com.java.PayGuard.common.exception.ResourceNotFoundException;
import com.java.PayGuard.expense.dto.ActivityInvoiceRequest;
import com.java.PayGuard.expense.dto.ActivityInvoiceResponse;
import com.java.PayGuard.expense.dto.InvoiceLineItemDto;
import com.java.PayGuard.expense.entity.ActivityInvoice;
import com.java.PayGuard.expense.entity.InvoiceLineItem;
import com.java.PayGuard.expense.repository.ActivityInvoiceRepository;
import com.java.PayGuard.expense.service.ActivityInvoiceService;
import com.java.PayGuard.user.entity.Role;
import com.java.PayGuard.user.entity.User;
import com.java.PayGuard.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityInvoiceServiceTest {

    @Mock
    private ActivityInvoiceRepository activityInvoiceRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActivityInvoiceService activityInvoiceService;

    private User testUser;
    private ActivityInvoice savedInvoice;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("freelancer@payguard.io")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        InvoiceLineItem incomeItem = InvoiceLineItem.builder()
                .id(1L)
                .type("INCOME")
                .description("Client Consulting Fee")
                .amount(new BigDecimal("3000.0000"))
                .category("SERVICES")
                .build();

        InvoiceLineItem expenseItem1 = InvoiceLineItem.builder()
                .id(2L)
                .type("EXPENSE")
                .description("Hotel Accommodation")
                .amount(new BigDecimal("450.0000"))
                .category("ACCOMMODATION")
                .build();

        InvoiceLineItem expenseItem2 = InvoiceLineItem.builder()
                .id(3L)
                .type("EXPENSE")
                .description("Flight Tickets")
                .amount(new BigDecimal("350.0000"))
                .category("TRAVEL")
                .build();

        savedInvoice = ActivityInvoice.builder()
                .id(201L)
                .user(testUser)
                .activityTitle("NYC Tech Conference 2026")
                .clientName("Acme Corp Inc.")
                .status("FINALIZED")
                .totalIncome(new BigDecimal("3000.0000"))
                .totalExpenses(new BigDecimal("800.0000"))
                .netProfit(new BigDecimal("2200.0000"))
                .profitMarginPercent(new BigDecimal("73.33"))
                .lineItems(List.of(incomeItem, expenseItem1, expenseItem2))
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: createInvoice — Net Profit & Margin Calculation
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should correctly calculate Net Profit and Profit Margin % for an Activity Invoice")
    void createInvoice_CalculatesNetProfitAndMargin_Correctly() {
        ActivityInvoiceRequest request = ActivityInvoiceRequest.builder()
                .activityTitle("NYC Tech Conference 2026")
                .clientName("Acme Corp Inc.")
                .status("FINALIZED")
                .lineItems(List.of(
                        InvoiceLineItemDto.builder().type("INCOME").description("Consulting Fee").amount(new BigDecimal("3000")).category("SERVICES").build(),
                        InvoiceLineItemDto.builder().type("EXPENSE").description("Hotel Stay").amount(new BigDecimal("450")).category("ACCOMMODATION").build(),
                        InvoiceLineItemDto.builder().type("EXPENSE").description("Flights").amount(new BigDecimal("350")).category("TRAVEL").build()
                ))
                .build();

        when(activityInvoiceRepository.save(any(ActivityInvoice.class))).thenReturn(savedInvoice);

        ActivityInvoiceResponse response = activityInvoiceService.createInvoice(request, testUser);

        assertNotNull(response);
        assertEquals(201L, response.getId());
        assertEquals("NYC Tech Conference 2026", response.getActivityTitle());
        assertEquals(new BigDecimal("3000.0000"), response.getTotalIncome());
        assertEquals(new BigDecimal("800.0000"), response.getTotalExpenses());
        assertEquals(new BigDecimal("2200.0000"), response.getNetProfit());

        // Margin: 2200/3000 * 100 = 73.33%
        assertEquals(0, new BigDecimal("73.33").compareTo(response.getProfitMarginPercent()));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: createInvoice — Zero income results in 0.00% margin (guard)
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return 0.00% profit margin when total income is zero (expense-only invoice)")
    void createInvoice_ZeroIncome_ZeroMarginPercent() {
        ActivityInvoice expenseOnlyInvoice = ActivityInvoice.builder()
                .id(202L)
                .user(testUser)
                .activityTitle("AWS Cost Report Q1 2026")
                .clientName("")
                .status("FINALIZED")
                .totalIncome(BigDecimal.ZERO)
                .totalExpenses(new BigDecimal("1200.0000"))
                .netProfit(new BigDecimal("-1200.0000"))
                .profitMarginPercent(BigDecimal.ZERO)
                .lineItems(List.of(
                        InvoiceLineItem.builder().id(10L).type("EXPENSE").description("AWS Compute").amount(new BigDecimal("1200.0000")).category("CLOUD").build()
                ))
                .build();

        ActivityInvoiceRequest request = ActivityInvoiceRequest.builder()
                .activityTitle("AWS Cost Report Q1 2026")
                .status("FINALIZED")
                .lineItems(List.of(
                        InvoiceLineItemDto.builder().type("EXPENSE").description("AWS Compute").amount(new BigDecimal("1200")).category("CLOUD").build()
                ))
                .build();

        when(activityInvoiceRepository.save(any(ActivityInvoice.class))).thenReturn(expenseOnlyInvoice);

        ActivityInvoiceResponse response = activityInvoiceService.createInvoice(request, testUser);

        assertEquals(BigDecimal.ZERO, response.getProfitMarginPercent());
        assertTrue(response.getNetProfit().compareTo(BigDecimal.ZERO) < 0); // Net loss
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: createInvoice — Rejects invalid line item type
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw IllegalArgumentException when line item type is invalid")
    void createInvoice_InvalidLineItemType_Throws() {
        ActivityInvoiceRequest request = ActivityInvoiceRequest.builder()
                .activityTitle("Bad Invoice")
                .status("FINALIZED")
                .lineItems(List.of(
                        InvoiceLineItemDto.builder().type("UNKNOWN_TYPE").description("???").amount(new BigDecimal("100")).build()
                ))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> activityInvoiceService.createInvoice(request, testUser));

        verify(activityInvoiceRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getInvoiceById — Owner can retrieve their invoice
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should return invoice details for the owning user")
    void getInvoiceById_Owner_ReturnsInvoice() {
        when(activityInvoiceRepository.findById(201L)).thenReturn(Optional.of(savedInvoice));

        ActivityInvoiceResponse response = activityInvoiceService.getInvoiceById(201L, testUser);

        assertNotNull(response);
        assertEquals(201L, response.getId());
        assertEquals("NYC Tech Conference 2026", response.getActivityTitle());
        assertEquals(3, response.getLineItems().size());
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getInvoiceById — Foreign user cannot access another user's invoice
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw IllegalArgumentException when foreign user tries to view another user's invoice")
    void getInvoiceById_UnauthorizedUser_Throws() {
        User otherUser = User.builder()
                .id(88L).email("hacker@evil.io")
                .role(Role.ROLE_CUSTOMER).status(UserStatus.ACTIVE).build();

        when(activityInvoiceRepository.findById(201L)).thenReturn(Optional.of(savedInvoice));

        assertThrows(IllegalArgumentException.class,
                () -> activityInvoiceService.getInvoiceById(201L, otherUser));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: getInvoiceById — Non-existent invoice
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw ResourceNotFoundException when activity invoice ID does not exist")
    void getInvoiceById_NotFound_Throws() {
        when(activityInvoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> activityInvoiceService.getInvoiceById(999L, testUser));
    }

    // ──────────────────────────────────────────────────────────────────────
    // TEST: generateCsvStatement — CSV content is non-empty and UTF-8 BOM present
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should generate a non-empty CSV statement with UTF-8 BOM and invoice data")
    void generateCsvStatement_ContainsInvoiceData() {
        when(activityInvoiceRepository.findById(201L)).thenReturn(Optional.of(savedInvoice));

        ByteArrayInputStream csv = activityInvoiceService.generateCsvStatement(201L, testUser);

        assertNotNull(csv);
        byte[] bytes = csv.readAllBytes();
        assertTrue(bytes.length > 0, "CSV should not be empty");

        // Verify UTF-8 BOM presence (0xEF, 0xBB, 0xBF)
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String csvContent = new String(bytes);
        assertTrue(csvContent.contains("NYC Tech Conference 2026"));
        assertTrue(csvContent.contains("TOTAL INCOME"));
        assertTrue(csvContent.contains("NET PROFIT"));
    }
}
