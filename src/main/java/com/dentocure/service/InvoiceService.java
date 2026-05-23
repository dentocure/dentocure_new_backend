package com.dentocure.service;

import com.dentocure.dto.InvoiceItemRequest;
import com.dentocure.dto.InvoiceRequest;
import com.dentocure.dto.PaymentRequest;
import com.dentocure.exception.ConflictException;
import com.dentocure.exception.ResourceNotFoundException;
import com.dentocure.model.Invoice;
import com.dentocure.model.InvoiceItem;
import com.dentocure.model.Payment;
import com.dentocure.repository.AppointmentRepository;
import com.dentocure.repository.InvoiceRepository;
import com.dentocure.repository.PatientRepository;
import com.dentocure.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class InvoiceService {

    /** Default GST rate — replace with ClinicSettings lookup once that module is built */
    private static final BigDecimal DEFAULT_GST_RATE = new BigDecimal("18.00");

    /** Invoice number prefix — replace with ClinicSettings once available */
    private static final String INVOICE_PREFIX = "INV-";

    private static final Set<String> VALID_PAYMENT_METHODS =
            Set.of("Cash", "UPI", "Card", "Net Banking", "Insurance");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    // ── Query ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoices(String status, String patientId,
                                     LocalDate dateFrom, LocalDate dateTo,
                                     String search, int page, int limit) {
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "date", "createdAt"));
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        return invoiceRepository.findWithFilters(status, patientId, dateFrom, dateTo, searchParam, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByPatient(String patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        return invoiceRepository.findByPatientIdOrderByDateDescCreatedAtDesc(patientId);
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    public Invoice createInvoice(InvoiceRequest request) {
        validatePatient(request.getPatientId());
        validateAppointment(request.getAppointmentId());

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        applyRequest(request, invoice);
        return invoiceRepository.save(invoice);
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    public Invoice updateInvoice(String id, InvoiceRequest request) {
        Invoice invoice = getInvoiceById(id);
        if (!"Draft".equals(invoice.getStatus())) {
            throw new IllegalArgumentException(
                    "Invoice '" + invoice.getInvoiceNumber() + "' cannot be edited — status is " + invoice.getStatus());
        }
        validatePatient(request.getPatientId());
        validateAppointment(request.getAppointmentId());
        applyRequest(request, invoice);
        return invoiceRepository.save(invoice);
    }

    // ── Void ───────────────────────────────────────────────────────────────────

    public void voidInvoice(String id) {
        Invoice invoice = getInvoiceById(id);
        if ("Void".equals(invoice.getStatus())) {
            throw new ConflictException("Invoice '" + invoice.getInvoiceNumber() + "' is already voided");
        }
        invoice.setStatus("Void");
        invoiceRepository.save(invoice);
    }

    // ── Payments ───────────────────────────────────────────────────────────────

    public Payment addPayment(String invoiceId, PaymentRequest request) {
        Invoice invoice = getInvoiceById(invoiceId);

        if ("Void".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("Cannot record a payment against a voided invoice");
        }
        if ("Paid".equals(invoice.getStatus())) {
            throw new ConflictException("Invoice '" + invoice.getInvoiceNumber() + "' is already fully paid");
        }
        if (!VALID_PAYMENT_METHODS.contains(request.getPaymentMethod())) {
            throw new IllegalArgumentException(
                    "Invalid payment method '" + request.getPaymentMethod() + "'. Allowed: " + VALID_PAYMENT_METHODS);
        }
        if (request.getAmount().compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount " + request.getAmount() + " exceeds balance due " + invoice.getBalanceDue());
        }

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setNotes(request.getNotes());
        paymentRepository.save(payment);

        // Recompute invoice totals
        BigDecimal newPaid = invoice.getPaidAmount().add(request.getAmount());
        invoice.setPaidAmount(newPaid);
        invoice.setBalanceDue(invoice.getGrandTotal().subtract(newPaid).setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(deriveStatus(invoice.getBalanceDue(), newPaid));
        invoiceRepository.save(invoice);

        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPayments(String invoiceId) {
        getInvoiceById(invoiceId); // ensure invoice exists
        return paymentRepository.findByInvoiceIdOrderByPaidAtAsc(invoiceId);
    }

    // ── PDF Stub ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Invoice getPdfInfo(String id) {
        return getInvoiceById(id);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void applyRequest(InvoiceRequest request, Invoice invoice) {
        invoice.setPatientId(request.getPatientId());
        invoice.setAppointmentId(request.getAppointmentId());
        invoice.setDate(request.getDate());
        invoice.setNotes(request.getNotes());

        // Compute line items and totals
        List<InvoiceItem> items = computeLineItems(request.getItems());
        invoice.setItems(serialiseItems(items));

        BigDecimal subtotal = items.stream()
                .map(InvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal gstRate = DEFAULT_GST_RATE;
        BigDecimal gstAmount = subtotal
                .multiply(gstRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(gstAmount).setScale(2, RoundingMode.HALF_UP);

        // Preserve paid amount when updating an existing Draft
        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balanceDue = grandTotal.subtract(paidAmount).setScale(2, RoundingMode.HALF_UP);

        invoice.setSubtotal(subtotal);
        invoice.setGstRate(gstRate);
        invoice.setGstAmount(gstAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setPaidAmount(paidAmount);
        invoice.setBalanceDue(balanceDue);

        // Status: honour explicit Draft/Unpaid from request, otherwise derive
        String requestedStatus = request.getStatus();
        if ("Draft".equals(requestedStatus) || requestedStatus == null) {
            // Keep as Draft unless invoice already has a non-Draft status
            if (invoice.getStatus() == null || "Draft".equals(invoice.getStatus())) {
                invoice.setStatus("Draft");
            }
        } else if ("Unpaid".equals(requestedStatus)) {
            invoice.setStatus(deriveStatus(balanceDue, paidAmount));
        }
    }

    private List<InvoiceItem> computeLineItems(List<InvoiceItemRequest> requests) {
        return requests.stream().map(req -> {
            InvoiceItem item = new InvoiceItem();
            item.setName(req.getName());
            item.setQty(req.getQty());
            item.setUnitPrice(req.getUnitPrice());
            BigDecimal discount = req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO;
            item.setDiscount(discount);
            BigDecimal lineTotal = req.getUnitPrice()
                    .multiply(new BigDecimal(req.getQty()))
                    .subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);
            item.setLineTotal(lineTotal);
            return item;
        }).toList();
    }

    /**
     * Derives invoice status from balance due.
     * "Draft" and "Void" are set explicitly — never auto-derived here.
     */
    private String deriveStatus(BigDecimal balanceDue, BigDecimal paidAmount) {
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            return "Paid";
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Partially Paid";
        }
        return "Unpaid";
    }

    /**
     * Generates the next sequential invoice number.
     * Format: INV-0001, INV-0002, ...
     * Uses MAX(invoice_number) query — safe for low-concurrency clinic usage.
     */
    private String generateInvoiceNumber() {
        Optional<String> maxOpt = invoiceRepository.findMaxInvoiceNumber(INVOICE_PREFIX);
        int next = 1;
        if (maxOpt.isPresent()) {
            String maxNumber = maxOpt.get(); // e.g. "INV-0042"
            try {
                String numericPart = maxNumber.substring(INVOICE_PREFIX.length());
                next = Integer.parseInt(numericPart) + 1;
            } catch (NumberFormatException ignored) {
                // fallback to count-based approach
                next = (int) invoiceRepository.count() + 1;
            }
        }
        return INVOICE_PREFIX + String.format("%04d", next);
    }

    private String serialiseItems(List<InvoiceItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise invoice items", e);
        }
    }

    private void validatePatient(String patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    private void validateAppointment(String appointmentId) {
        if (appointmentId != null && !appointmentId.isBlank()) {
            appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        }
    }
}
