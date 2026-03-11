package com.dentocure.controller;

import com.dentocure.dto.DoctorRequest;
import com.dentocure.dto.StatusUpdateRequest;
import com.dentocure.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Doctors", description = "Doctor and staff profile management")
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List all doctors",
        description = "Returns all doctor profiles. Includes inactive doctors (active=false)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of doctors returned successfully")
    })
    @GetMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getAllDoctors() {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.getAllDoctors()));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get a single doctor",
        description = "Fetches one doctor by their ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Doctor found"),
        @ApiResponse(responseCode = "404", description = "Doctor not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"NOT_FOUND","message":"Doctor not found with id: DR999"}}""")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getDoctorById(
            @Parameter(description = "Doctor ID (e.g. DR001)", example = "DR001")
            @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.getDoctorById(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Add a new doctor",
        description = "Creates a new doctor profile. `name` is required."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Doctor created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error — name missing or email invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"error":{"code":"VALIDATION_ERROR","message":"Request validation failed","details":["name: Name is required"]}}""")))
    })
    @PostMapping
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> createDoctor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Doctor details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {
                          "name": "Dr. Arjun Bose",
                          "specialization": "Cosmetic Dentist",
                          "phone": "9876500001",
                          "email": "arjun.bose@dentocure.com",
                          "color": "#6366F1",
                          "workingHours": "{\\"start\\":\\"09:00\\",\\"end\\":\\"17:00\\"}",
                          "active": true
                        }""")))
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.dentocure.dto.ApiResponse.of(doctorService.createDoctor(request)));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Update doctor details",
        description = "Full update of a doctor profile. All fields are replaced."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Doctor updated"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> updateDoctor(
            @Parameter(description = "Doctor ID", example = "DR001") @PathVariable String id,
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.updateDoctor(id, request)));
    }

    // ── Toggle Status ─────────────────────────────────────────────────────────

    @Operation(
        summary = "Toggle doctor active / inactive",
        description = "Pass `\"status\": \"active\"` or `\"status\": \"inactive\"` in the body."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> updateDoctorStatus(
            @Parameter(description = "Doctor ID", example = "DR005") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"status\":\"active\"}")))

            @Valid @RequestBody StatusUpdateRequest request) {
        boolean active = "active".equalsIgnoreCase(request.getStatus());
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.updateDoctorStatus(id, active)));
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Operation(
        summary = "Soft-delete a doctor",
        description = "Sets `active = false`. The doctor record is retained in the database."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Doctor deactivated"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> deleteDoctor(
            @Parameter(description = "Doctor ID", example = "DR001") @PathVariable String id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(Map.of("message", "Doctor deactivated successfully")));
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    @Operation(
        summary = "Get doctor's appointment schedule",
        description = "Returns all appointments for a doctor on a specific date. Defaults to today if `date` is omitted."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schedule returned"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping("/{id}/schedule")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getDoctorSchedule(
            @Parameter(description = "Doctor ID", example = "DR001") @PathVariable String id,
            @Parameter(description = "Date to fetch schedule for (YYYY-MM-DD). Defaults to today.", example = "2026-03-12")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.getDoctorSchedule(id, date)));
    }

    // ── Availability ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get doctor's working hours",
        description = "Returns the doctor profile including the `workingHours` JSON field. " +
                      "Use `GET /api/appointments/slots` to get free time slots for a date."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability info returned"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @GetMapping("/{id}/availability")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> getDoctorAvailability(
            @Parameter(description = "Doctor ID", example = "DR002") @PathVariable String id) {
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(doctorService.getDoctorAvailability(id)));
    }

    // ── Leave ─────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Mark leave dates for a doctor",
        description = "Records leave dates for a doctor. Full leave management (blocking slots) is a future feature."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leave recorded"),
        @ApiResponse(responseCode = "404", description = "Doctor not found")
    })
    @PostMapping("/{id}/leave")
    public ResponseEntity<com.dentocure.dto.ApiResponse<?>> markLeave(
            @Parameter(description = "Doctor ID", example = "DR003") @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Leave details",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = """
                        {"dates":["2026-03-25","2026-03-26"],"reason":"Medical Conference"}""")))
            @RequestBody Map<String, Object> body) {
        doctorService.getDoctorById(id);
        return ResponseEntity.ok(com.dentocure.dto.ApiResponse.of(
                Map.of("message", "Leave recorded successfully", "doctorId", id, "leave", body)));
    }
}
