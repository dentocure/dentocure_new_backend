package com.dentocure.repository;

import com.dentocure.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    /** All appointments for a given doctor on a specific date (used for conflict detection & schedule) */
    List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDate date);

    /** All appointments for a specific patient */
    List<Appointment> findByPatientIdOrderByDateDescTimeDesc(String patientId);

    /** All appointments on a specific date (used for "today" endpoint) */
    List<Appointment> findByDateOrderByTimeAsc(LocalDate date);

    /**
     * Filterable, paginated appointment listing.
     * All filter params are optional (null = not applied).
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(:date IS NULL OR a.date = :date) AND " +
           "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:patientId IS NULL OR a.patientId = :patientId)")
    Page<Appointment> findWithFilters(@Param("date") LocalDate date,
                                      @Param("doctorId") String doctorId,
                                      @Param("status") String status,
                                      @Param("type") String type,
                                      @Param("patientId") String patientId,
                                      Pageable pageable);
}
