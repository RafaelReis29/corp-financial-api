package com.senac.corpfinancialapi;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Page<Company> findAllByAccount_Id(UUID accountId, Pageable pageable);

    boolean existsByIdAndAccount_Id(Long id, UUID accountId);

    Optional<Company> findByIdAndAccount_Id(Long id, UUID accountId);
}
