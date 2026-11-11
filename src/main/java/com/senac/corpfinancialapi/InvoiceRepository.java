package com.senac.corpfinancialapi;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByContract_Company_Account_Id(UUID accountId, Pageable pageable);

    Optional<Invoice> findByIdAndContract_Company_Account_Id(Long id, UUID accountId);
}
