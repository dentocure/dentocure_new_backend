package com.dentocure.controller;

import com.dentocure.dto.ApiResponse;
import com.dentocure.dto.InvoiceRequest;
import com.dentocure.dto.PageMeta;
import com.dentocure.dto.PaymentRequest;
import com.dentocure.model.Invoice;
import com.dentocure.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Billing & Invoicing", description = "Invoice creation, payment recording, and PDF generation")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List invoices",
        description = """
            Returns a paginated, filterable list of invoices sorted by date descending.

            **Filters (all optional):**
            - `status` — `Draft` | `Unpaid` | `Partially Paid` | `Paid` | `Void`
            - `patientId` — filter by patient
            - `dateFrom` / `dateTo` — date range (YYYY-MM-DD)
            - `search` — match against invoice number (e.g. `INV-001`)
            - `page` / `limit` — pagination (default 1 / 20, max 100)
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice list returned with pagination meta")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getInvoices(
            @Parameter(description = "Filter by status",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"Draft", "Unpaid", "Partially Paid", "Paid", "Void"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by patient ID", example = "PT001")
            @RequestParam(required = false) String patientId,
            @Parameter(description = "Start of date range (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "End of date range (YYYY-MM-DD)", example = "2026-05-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "Search by invoice number", example = "INV-0010")
            @RequestParam(required = false) String search,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Page<Invoice> result = invoiceService.getInvoices(status, patientId, dateFrom, dateTo, search, page, limit);
        return ResponseEntity.ok(
                ApiResponse.of(result.getContent(), new PageMeta(page, limit, result.getTotalElements())));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get a single invoice",
        description = "Fetches one invoice by its ID, including all line items and computed totals."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"NOT_FOUND","message":"Invoice not found with id: abc123"}}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getInvoiceById(
            @Parameter(description = "Invoice ID", example = "abc-123") @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.getInvoiceById(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Create a new invoice",
        description = """
            Creates an invoice with server-side computation of all financial fields:

            ```
            lineTotal   = qty × unitPrice − discount
            subtotal    = Σ lineTotals
            gstAmount   = subtotal × 18%
            grandTotal  = subtotal + gstAmount
            balanceDue  = grandTotal − paidAmount
            ```

            - Invoice number is auto-generated (e.g. `INV-0001`)
            - GST rate is snapshotted at creation (currently 18%)
            - Default status is `Draft`; pass `"status": "Unpaid"` to finalise immediately
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invoice created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Patient or Appointment not found")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createInvoice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Invoice details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {
                          "patientId": "PT001",
                          "appointmentId": "AP001",
                          "date": "2026-05-21",
                          "items": [
                            { "name": "Root Canal Treatment", "qty": 1, "unitPrice": 3500.00, "discount": 200.00 },
                            { "name": "X-Ray", "qty": 2, "unitPrice": 300.00 }
                          ],
                          "notes": "Post-treatment care included",
                          "status": "Draft"
                        }""")))
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(invoiceService.createInvoice(request)));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Update an invoice",
        description = """
            Full update of an invoice. **Only allowed when status is `Draft`.**
            All financial fields are recomputed server-side from the new line items.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invoice is not in Draft status",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"INVALID_REQUEST","message":"Invoice 'INV-0001' cannot be edited — status is Paid"}}"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateInvoice(
            @Parameter(description = "Invoice ID") @PathVariable String id,
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.updateInvoice(id, request)));
    }

    // ── Void ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Void an invoice",
        description = "Sets the invoice status to `Void`. The record is retained for audit history."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice voided"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invoice is already voided")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> voidInvoice(
            @Parameter(description = "Invoice ID") @PathVariable String id) {
        invoiceService.voidInvoice(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Invoice voided successfully")));
    }

    // ── Record Payment ────────────────────────────────────────────────────────

    @Operation(
        summary = "Record a payment against an invoice",
        description = """
            Records a partial or full payment. After saving the payment:
            - `paidAmount` is incremented
            - `balanceDue` is recomputed
            - Invoice `status` is auto-derived: `Paid` / `Partially Paid`

            **Rejected when:**
            - Invoice is `Void`
            - Invoice is already `Paid`
            - Payment amount exceeds `balanceDue`
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment recorded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payment method or amount exceeds balance",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"INVALID_REQUEST","message":"Payment amount 5000.00 exceeds balance due 3200.00"}}"""))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Invoice is already fully paid or voided")
    })
    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<?>> addPayment(
            @Parameter(description = "Invoice ID") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Payment details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {
                          "amount": 1500.00,
                          "paymentMethod": "UPI",
                          "notes": "Google Pay — ref #XYZ123"
                        }""")))
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(invoiceService.addPayment(id, request)));
    }

    // ── List Payments ─────────────────────────────────────────────────────────

    @Operation(
        summary = "List all payments for an invoice",
        description = "Returns all payment records for the given invoice, sorted by payment date ascending."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment list returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<?>> getPayments(
            @Parameter(description = "Invoice ID") @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.of(invoiceService.getPayments(id)));
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get PDF for an invoice",
        description = """
            Returns the `pdfUrl` for the invoice if a PDF has been generated.
            Full PDF generation (HTML → PDF via a rendering library) is not yet implemented.
            The `pdfUrl` field will be `null` until this feature is built.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF info returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/{id}/pdf")
    public ResponseEntity<ApiResponse<?>> getInvoicePdf(
            @Parameter(description = "Invoice ID") @PathVariable String id) {
        Invoice invoice = invoiceService.getPdfInfo(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of(
                "invoiceNumber", invoice.getInvoiceNumber(),
                "pdfUrl", invoice.getPdfUrl() != null ? invoice.getPdfUrl() : "",
                "message", invoice.getPdfUrl() != null
                        ? "PDF available"
                        : "PDF generation not yet implemented"
        )));
    }
}
