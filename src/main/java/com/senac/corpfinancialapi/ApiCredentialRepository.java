package com.senac.corpfinancialapi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface ApiCredentialRepository extends JpaRepository<ApiCredential, UUID> {

    @EntityGraph(attributePaths = "account")
    Optional<ApiCredential> findBySecretHash(String secretHash);

    @Override
    @EntityGraph(attributePaths = "account")
    Optional<ApiCredential> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "account")
    List<ApiCredential> findAll(Sort sort);

    @Transactional
    @Modifying
    @Query("delete from ApiCredential credential where credential.id = :id")
    int revoke(UUID id);
}
