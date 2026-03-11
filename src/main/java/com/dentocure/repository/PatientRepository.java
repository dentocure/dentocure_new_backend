package com.dentocure.repository;

import com.dentocure.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    Optional<Patient> findByPhone(String phone);

    /**
     * Filterable patient listing:
     * - active: null = all, true/false = filtered
     * - search: matches name (case-insensitive) or phone (partial)
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(:search IS NULL OR " +
           "  LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  p.phone LIKE CONCAT('%', :search, '%'))")
    Page<Patient> findWithFilters(@Param("active") Boolean active,
                                  @Param("search") String search,
                                  Pageable pageable);
}
