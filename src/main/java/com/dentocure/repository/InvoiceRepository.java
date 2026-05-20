package com.dentocure.repository;

import com.dentocure.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /** All invoices for a specific patient, newest first */
    List<Invoice> findByPatientIdOrderByDateDescCreatedAtDesc(String patientId);

    /**
     * Filterable, paginated invoice listing.
     * All filter params are optional (null = not applied).
     * search matches patient ID prefix or invoice number substring (service layer resolves patientId from name search).
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:patientId IS NULL OR i.patientId = :patientId) AND " +
           "(:dateFrom IS NULL OR i.date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR i.date <= :dateTo) AND " +
           "(:search IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> findWithFilters(@Param("status") String status,
                                  @Param("patientId") String patientId,
                                  @Param("dateFrom") LocalDate dateFrom,
                                  @Param("dateTo") LocalDate dateTo,
                                  @Param("search") String search,
                                  Pageable pageable);

    /**
     * Returns the highest numeric suffix currently in use so the service
     * can derive the next invoice number (e.g. "INV-0042" → 42 → next = "INV-0043").
     */
    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxInvoiceNumber(@Param("prefix") String prefix);
}
