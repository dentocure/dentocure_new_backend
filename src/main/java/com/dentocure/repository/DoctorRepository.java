package com.dentocure.repository;

import com.dentocure.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {

    List<Doctor> findByActive(boolean active);

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByPhone(String phone);
}
