package com.dentocure.service;

import com.dentocure.dto.PatientRequest;
import com.dentocure.exception.ConflictException;
import com.dentocure.exception.ResourceNotFoundException;
import com.dentocure.model.Appointment;
import com.dentocure.model.Patient;
import com.dentocure.repository.AppointmentRepository;
import com.dentocure.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Paginated, searchable patient listing.
     *
     * @param search  optional name/phone substring
     * @param page    1-based page number
     * @param limit   page size (capped at 100)
     * @param active  null = all, true/false = filtered
     */
    @Transactional(readOnly = true)
    public Page<Patient> getPatients(String search, int page, int limit, Boolean active) {
        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        return patientRepository.findWithFilters(active, searchParam, pageable);
    }

    @Transactional(readOnly = true)
    public Patient getPatientById(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    /** Creates a patient — enforces phone uniqueness across active patients */
    public Patient createPatient(PatientRequest request) {
        patientRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
            if (existing.isActive()) {
                throw new ConflictException(
                        "A patient with phone number " + request.getPhone() + " already exists");
            }
        });

        Patient patient = new Patient();
        applyRequest(request, patient);
        return patientRepository.save(patient);
    }

    /** Updates a patient — enforces phone uniqueness excluding the current patient */
    public Patient updatePatient(String id, PatientRequest request) {
        Patient patient = getPatientById(id);

        patientRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && existing.isActive()) {
                throw new ConflictException(
                        "Phone number " + request.getPhone() + " is already used by another patient");
            }
        });

        applyRequest(request, patient);
        return patientRepository.save(patient);
    }

    /** Soft-delete: sets active = false. Patient records are never hard-deleted. */
    public void deletePatient(String id) {
        Patient patient = getPatientById(id);
        patient.setActive(false);
        patientRepository.save(patient);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(String id) {
        getPatientById(id); // validate existence
        return appointmentRepository.findByPatientIdOrderByDateDescTimeDesc(id);
    }

    private void applyRequest(PatientRequest req, Patient patient) {
        patient.setName(req.getName().trim());
        patient.setPhone(req.getPhone().trim());
        patient.setEmail(req.getEmail());
        patient.setDob(req.getDob());
        patient.setGender(req.getGender());
        patient.setBloodGroup(req.getBloodGroup());
        patient.setAllergies(req.getAllergies() != null ? req.getAllergies() : new ArrayList<>());
        patient.setEmergContact(req.getEmergContact());
        patient.setReferredBy(req.getReferredBy());
        patient.setNotes(req.getNotes());
    }
}
