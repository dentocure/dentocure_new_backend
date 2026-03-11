package com.dentocure.service;

import com.dentocure.dto.AppointmentRequest;
import com.dentocure.exception.ConflictException;
import com.dentocure.exception.ResourceNotFoundException;
import com.dentocure.model.Appointment;
import com.dentocure.model.Doctor;
import com.dentocure.repository.AppointmentRepository;
import com.dentocure.repository.DoctorRepository;
import com.dentocure.repository.PatientRepository;
import org.springframework.data.domain.Page;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class AppointmentService {

    private static final Set<String> VALID_STATUSES =
            Set.of("Scheduled", "In-Chair", "Completed", "Cancelled", "No-show");

    /** Default clinic working hours when doctor's workingHours is not configured */
    private static final LocalTime DEFAULT_WORK_START = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_WORK_END   = LocalTime.of(17, 0);
    private static final int SLOT_DURATION_MINUTES    = 30;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    public AppointmentService(AppointmentRepository appointmentRepository,
                               DoctorRepository doctorRepository,
                               PatientRepository patientRepository,
                               ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<Appointment> getAppointments(LocalDate date, String doctorId, String status,
                                              String type, String patientId, int page, int limit) {
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("date", "time"));
        return appointmentRepository.findWithFilters(date, doctorId, status, type, patientId, pageable);
    }

    @Transactional(readOnly = true)
    public Appointment getAppointmentById(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    public Appointment createAppointment(AppointmentRequest request) {
        validateForeignKeys(request);
        validateDuration(request.getDuration());
        checkConflict(request.getDoctorId(), request.getDate(), request.getTime(),
                request.getDuration(), null);

        Appointment appointment = new Appointment();
        applyRequest(request, appointment);
        return appointmentRepository.save(appointment);
    }

    public Appointment updateAppointment(String id, AppointmentRequest request) {
        getAppointmentById(id); // validate existence
        validateForeignKeys(request);
        validateDuration(request.getDuration());
        checkConflict(request.getDoctorId(), request.getDate(), request.getTime(),
                request.getDuration(), id);

        Appointment appointment = getAppointmentById(id);
        applyRequest(request, appointment);
        return appointmentRepository.save(appointment);
    }

    /** PATCH /api/appointments/{id}/status */
    public Appointment updateStatus(String id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Invalid status '" + status + "'. Allowed: " + VALID_STATUSES);
        }
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    /** DELETE /api/appointments/{id} — sets status to Cancelled */
    public void cancelAppointment(String id) {
        Appointment appointment = getAppointmentById(id);
        appointment.setStatus("Cancelled");
        appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getTodayAppointments() {
        return appointmentRepository.findByDateOrderByTimeAsc(LocalDate.now());
    }

    /**
     * Returns all 30-minute slots within the doctor's working hours for a given date,
     * marking each slot as available or booked.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableSlots(String doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        // Parse doctor's configured working hours, fall back to clinic defaults
        LocalTime workStart = DEFAULT_WORK_START;
        LocalTime workEnd   = DEFAULT_WORK_END;
        try {
            if (doctor.getWorkingHours() != null && !doctor.getWorkingHours().isBlank()) {
                JsonNode wh = objectMapper.readTree(doctor.getWorkingHours());
                workStart = LocalTime.parse(wh.get("start").asText());
                workEnd   = LocalTime.parse(wh.get("end").asText());
            }
        } catch (Exception ignored) {
            // malformed JSON — use defaults
        }

        final LocalTime finalWorkEnd = workEnd;
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        List<Map<String, Object>> slots = new ArrayList<>();
        LocalTime cursor = workStart;

        while (!cursor.plusMinutes(SLOT_DURATION_MINUTES).isAfter(finalWorkEnd)) {
            final LocalTime slotStart = cursor;
            final LocalTime slotEnd   = cursor.plusMinutes(SLOT_DURATION_MINUTES);

            boolean occupied = existing.stream()
                    .filter(a -> !a.getStatus().equals("Cancelled") && !a.getStatus().equals("No-show"))
                    .anyMatch(a -> overlaps(a.getTime(), a.getDuration(), slotStart, SLOT_DURATION_MINUTES));

            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("time", slotStart.toString());
            slot.put("endTime", slotEnd.toString());
            slot.put("available", !occupied);
            slots.add(slot);

            cursor = slotEnd;
        }

        return slots;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void validateForeignKeys(AppointmentRequest req) {
        patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", req.getPatientId()));
        doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", req.getDoctorId()));
    }

    private void validateDuration(int duration) {
        if (duration % 15 != 0) {
            throw new IllegalArgumentException("Duration must be a multiple of 15 minutes");
        }
    }

    /**
     * Prevents double-booking a doctor.
     * Two appointments overlap when: startA < endB AND startB < endA
     *
     * @param excludeId appointment ID to exclude from the check (used on updates)
     */
    private void checkConflict(String doctorId, LocalDate date, LocalTime time,
                                int duration, String excludeId) {
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndDate(doctorId, date);

        boolean conflict = existing.stream()
                .filter(a -> !a.getStatus().equals("Cancelled") && !a.getStatus().equals("No-show"))
                .filter(a -> excludeId == null || !a.getId().equals(excludeId))
                .anyMatch(a -> overlaps(a.getTime(), a.getDuration(), time, duration));

        if (conflict) {
            throw new ConflictException(
                    "Doctor already has an appointment overlapping the requested time slot");
        }
    }

    private boolean overlaps(LocalTime startA, int durationA, LocalTime startB, int durationB) {
        int a1 = startA.toSecondOfDay() / 60;
        int a2 = a1 + durationA;
        int b1 = startB.toSecondOfDay() / 60;
        int b2 = b1 + durationB;
        return b1 < a2 && a1 < b2;
    }

    private void applyRequest(AppointmentRequest req, Appointment appt) {
        appt.setPatientId(req.getPatientId());
        appt.setDoctorId(req.getDoctorId());
        appt.setType(req.getType());
        appt.setDate(req.getDate());
        appt.setTime(req.getTime());
        appt.setDuration(req.getDuration());
        appt.setStatus(req.getStatus() != null ? req.getStatus() : "Scheduled");
        appt.setNotes(req.getNotes());
        appt.setEmergency(req.isEmergency());
    }
}
