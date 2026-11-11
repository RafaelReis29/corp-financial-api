package com.senac.corpfinancialapi;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Page<PaymentMethod> findAllByCompany_Account_Id(UUID accountId, Pageable pageable);

    boolean existsByIdAndCompany_Account_Id(Long id, UUID accountId);

    Optional<PaymentMethod> findByIdAndCompany_Account_Id(Long id, UUID accountId);
}
