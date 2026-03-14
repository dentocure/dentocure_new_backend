package com.dentocure.controller;

import com.dentocure.dto.AppointmentRequest;
import com.dentocure.dto.PageMeta;
import com.dentocure.dto.StatusUpdateRequest;
import com.dentocure.model.Appointment;
import com.dentocure.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Appointments", description = "Appointment scheduling and status tracking")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List appointments",
        description = """
            Returns a paginated, filterable list of appointments sorted by date and time.

            **Filters (all optional):**
            - `date` — exact date match (YYYY-MM-DD)
            - `doctorId` — filter by doctor
            - `patientId` — filter by patient
            - `status` — `Scheduled` | `In-Chair` | `Completed` | `Cancelled` | `No-show`
            - `type` — appointment type label (e.g. `Checkup`, `Root Canal`)
            - `page` / `limit` — pagination (default 1 / 20, max 100)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment list returned with pagination meta")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    @GetMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getAppointments(
            @Parameter(description = "Filter by exact date (YYYY-MM-DD)", example = "2026-03-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Filter by doctor ID", example = "DR001")
            @RequestParam(required = false) String doctorId,
            @Parameter(description = "Filter by status", example = "Scheduled",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"Scheduled", "In-Chair", "Completed", "Cancelled", "No-show"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by appointment type", example = "Checkup")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filter by patient ID", example = "PT001")
            @RequestParam(required = false) String patientId,
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit) {

        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100;

        Page<Appointment> result = appointmentService.getAppointments(
                date, doctorId, status, type, patientId, page, limit);
        return ResponseEntity.ok(
                com.dentocure.dto.ApiResponse.of(result.getContent(), new PageMeta(page, limit, result.getTotalElements())));
    }

    // ── Today ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get today's appointments",
        description = "Returns all appointments for today sorted by time ascending. Equivalent to `GET /api/appointments?date=<today>`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Today's appointments returned")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    @GetMapping("/today")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getTodayAppointments() {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(appointmentService.getTodayAppointments()));
    }

    // ── Available Slots ───────────────────────────────────────────────────────

    @Operation(
        summary = "Get available time slots for a doctor",
        description = """
            Returns all 30-minute slots within the doctor's working hours for the given date.
            Each slot shows `available: true/false` based on existing non-cancelled appointments.

            **Sample response entry:**
            ```json
            {"time":"09:00","endTime":"09:30","available":false}
            ```
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Slot list returned"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    @GetMapping("/slots")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getAvailableSlots(
            @Parameter(description = "Doctor ID to check slots for", required = true, example = "DR001")
            @RequestParam String doctorId,
            @Parameter(description = "Date to check (YYYY-MM-DD)", required = true, example = "2026-03-12")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(appointmentService.getAvailableSlots(doctorId, date)));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get a single appointment",
        description = "Fetches one appointment by its ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment found"),
        @ApiResponse(responseCode = "404", description = "Appointment not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"NOT_FOUND","message":"Appointment not found with id: AP999"}}""")))
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    @GetMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getAppointmentById(
            @Parameter(description = "Appointment ID (e.g. AP001)", example = "AP001")
            @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(appointmentService.getAppointmentById(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Create a new appointment",
        description = """
            Books a new appointment with the following validations:
            - **Conflict detection** — rejects if the doctor already has an overlapping appointment
            - **Duration** — must be a multiple of 15 minutes (e.g. 15, 30, 45, 60)
            - **Patient & Doctor** — both IDs must exist in the database
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Appointment created"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid duration",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"INVALID_REQUEST","message":"Duration must be a multiple of 15 minutes"}}"""))),
        @ApiResponse(responseCode = "404", description = "Patient or Doctor not found"),
        @ApiResponse(responseCode = "409", description = "Booking conflict — doctor is already occupied",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"CONFLICT","message":"Doctor already has an appointment overlapping the requested time slot"}}""")))
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    @PostMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> createAppointment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Appointment details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {
                          "patientId": "PT001",
                          "doctorId": "DR002",
                          "type": "Root Canal",
                          "date": "2026-03-13",
                          "time": "10:00",
                          "duration": 60,
                          "notes": "Second session",
                          "emergency": false
                        }""")))
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.dentocure.dto.ApiResponse.of(appointmentService.createAppointment(request)));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Update an appointment",
        description = "Full update of an appointment. Conflict detection and duration validation are re-run, excluding the current appointment from the conflict check."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment updated"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "409", description = "Booking conflict")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    @PutMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> updateAppointment(
            @Parameter(description = "Appointment ID", example = "AP001") @PathVariable String id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(appointmentService.updateAppointment(id, request)));
    }

    // ── Update Status ─────────────────────────────────────────────────────────

    @Operation(
        summary = "Update appointment status",
        description = """
            Updates only the `status` field of an appointment.

            **Valid status values and transitions:**
            ```
            Scheduled → In-Chair → Completed
            Scheduled → No-show
            Scheduled → Cancelled
            ```
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"INVALID_REQUEST","message":"Invalid status 'Done'. Allowed: [Scheduled, In-Chair, Completed, Cancelled, No-show]"}}"""))),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('RECEPTIONIST')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> updateStatus(
            @Parameter(description = "Appointment ID", example = "AP001") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = {
                        @ExampleObject(name = "In-Chair",  value = "{\"status\":\"In-Chair\"}"),
                        @ExampleObject(name = "Completed", value = "{\"status\":\"Completed\"}"),
                        @ExampleObject(name = "No-show",   value = "{\"status\":\"No-show\"}"),
                        @ExampleObject(name = "Cancelled", value = "{\"status\":\"Cancelled\"}")
                    }))
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(appointmentService.updateStatus(id, request.getStatus())));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Cancel an appointment",
        description = "Sets the appointment status to `Cancelled`. The record is retained for history."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Appointment cancelled"),
        @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    @DeleteMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> cancelAppointment(
            @Parameter(description = "Appointment ID", example = "AP001") @PathVariable String id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(Map.of("message", "Appointment cancelled successfully")));
    }
}
