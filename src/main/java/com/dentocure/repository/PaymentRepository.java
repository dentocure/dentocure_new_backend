package com.dentocure.repository;

import com.dentocure.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** All payments for a given invoice, oldest first */
    List<Payment> findByInvoiceIdOrderByPaidAtAsc(String invoiceId);
}
