package com.senac.corpfinancialapi;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    Page<Contract> findAllByCompany_Account_Id(UUID accountId, Pageable pageable);

    boolean existsByIdAndCompany_Account_Id(Long id, UUID accountId);

    Optional<Contract> findByIdAndCompany_Account_Id(Long id, UUID accountId);
}
