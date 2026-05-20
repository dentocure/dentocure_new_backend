package com.dentocure.controller;

import com.dentocure.dto.PageMeta;
import com.dentocure.dto.PatientRequest;
import com.dentocure.model.Patient;
import com.dentocure.service.InvoiceService;
import com.dentocure.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Patients", description = "Patient registration and medical records")
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final InvoiceService invoiceService;

    public PatientController(PatientService patientService, InvoiceService invoiceService) {
        this.patientService = patientService;
        this.invoiceService = invoiceService;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List patients",
        description = """
            Returns a paginated, searchable list of patients.

            **Filters:**
            - `search` — partial match on name or phone number
            - `active` — `true` returns active patients, `false` returns soft-deleted
            - `page` / `limit` — pagination (default 1 / 20, max limit 100)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient list returned with pagination meta")
    })
    @GetMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getPatients(
            @Parameter(description = "Search by name or phone (partial match)", example = "ravi")
            @RequestParam(required = false) String search,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Filter by active status. Omit for all patients.", example = "true")
            @RequestParam(required = false) Boolean active) {

        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Page<Patient> result = patientService.getPatients(search, page, limit, active);
        return ResponseEntity.ok(
                com.dentocure.dto.ApiResponse.of(result.getContent(), new PageMeta(page, limit, result.getTotalElements())));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get a single patient",
        description = "Fetches one patient by ID. The response includes `age` computed server-side from `dob`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient found"),
        @ApiResponse(responseCode = "404", description = "Patient not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"NOT_FOUND","message":"Patient not found with id: PT999"}}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getPatientById(
            @Parameter(description = "Patient ID (e.g. PT001)", example = "PT001")
            @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(patientService.getPatientById(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Create a new patient",
        description = "Registers a new patient. `name` and `phone` are required. Phone must be unique across active patients."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient created"),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"VALIDATION_ERROR","message":"Request validation failed","details":["phone: Phone number is required"]}}"""))),
        @ApiResponse(responseCode = "409", description = "Phone number already in use",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"CONFLICT","message":"A patient with phone number 9811111111 already exists"}}""")))
    })
    @PostMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> createPatient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Patient registration details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {
                          "name": "Anil Kapoor",
                          "phone": "9812345678",
                          "email": "anil.kapoor@example.com",
                          "dob": "1980-05-10",
                          "gender": "Male",
                          "bloodGroup": "O+",
                          "allergies": ["Penicillin"],
                          "emergContact": "9812345679",
                          "referredBy": "Walk-in",
                          "notes": "First visit"
                        }""")))
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.dentocure.dto.ApiResponse.of(patientService.createPatient(request)));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Update patient details",
        description = "Full update of a patient record. Phone uniqueness is re-validated (excluding the current patient)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient updated"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "409", description = "Phone already used by another patient")
    })
    @PutMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> updatePatient(
            @Parameter(description = "Patient ID", example = "PT001") @PathVariable String id,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(patientService.updatePatient(id, request)));
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Operation(
        summary = "Soft-delete a patient",
        description = "Sets `active = false`. Patient records are **never** hard-deleted to preserve medical history."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient deactivated"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> deletePatient(
            @Parameter(description = "Patient ID", example = "PT001") @PathVariable String id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(Map.of("message", "Patient deactivated successfully")));
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all appointments for a patient",
        description = "Returns appointment history for a patient, sorted by date and time descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointments returned"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/{id}/appointments")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getPatientAppointments(
            @Parameter(description = "Patient ID", example = "PT001") @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(patientService.getPatientAppointments(id)));
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get all invoices for a patient",
        description = "Returns invoice history for a patient, sorted by date descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoices returned"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/{id}/invoices")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getPatientInvoices(
            @Parameter(description = "Patient ID", example = "PT001") @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(invoiceService.getInvoicesByPatient(id)));
    }

    // ── Reminders (stub) ──────────────────────────────────────────────────────

    @Operation(
        summary = "Get reminder history for a patient",
        description = "**Not yet implemented.** Returns an empty list. Reminders module is planned for v1.1."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Empty list (module not yet implemented)"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/{id}/reminders")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getPatientReminders(
            @Parameter(description = "Patient ID", example = "PT001") @PathVariable String id) {
        patientService.getPatientById(id);
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(List.of()));
    }
}
