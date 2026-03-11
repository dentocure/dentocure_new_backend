package com.dentocure.service;

import com.dentocure.dto.DoctorRequest;
import com.dentocure.exception.ResourceNotFoundException;
import com.dentocure.model.Appointment;
import com.dentocure.model.Doctor;
import com.dentocure.repository.AppointmentRepository;
import com.dentocure.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorById(String id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
    }

    public Doctor createDoctor(DoctorRequest request) {
        Doctor doctor = new Doctor();
        applyRequest(request, doctor);
        return doctorRepository.save(doctor);
    }

    public Doctor updateDoctor(String id, DoctorRequest request) {
        Doctor doctor = getDoctorById(id);
        applyRequest(request, doctor);
        return doctorRepository.save(doctor);
    }

    /** PATCH /api/doctors/{id}/status — toggle active flag */
    public Doctor updateDoctorStatus(String id, boolean active) {
        Doctor doctor = getDoctorById(id);
        doctor.setActive(active);
        return doctorRepository.save(doctor);
    }

    /** Soft-delete: sets active = false */
    public void deleteDoctor(String id) {
        Doctor doctor = getDoctorById(id);
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    /** GET /api/doctors/{id}/schedule — appointments for a doctor on a given date */
    @Transactional(readOnly = true)
    public List<Appointment> getDoctorSchedule(String id, LocalDate date) {
        getDoctorById(id); // validate existence
        LocalDate target = date != null ? date : LocalDate.now();
        return appointmentRepository.findByDoctorIdAndDate(id, target);
    }

    /** GET /api/doctors/{id}/availability — returns doctor with embedded workingHours */
    @Transactional(readOnly = true)
    public Doctor getDoctorAvailability(String id) {
        return getDoctorById(id);
    }

    private void applyRequest(DoctorRequest req, Doctor doctor) {
        doctor.setName(req.getName());
        doctor.setSpecialization(req.getSpecialization());
        doctor.setPhone(req.getPhone());
        doctor.setEmail(req.getEmail());
        doctor.setColor(req.getColor());
        doctor.setWorkingHours(req.getWorkingHours());
        doctor.setActive(req.isActive());
    }
}
