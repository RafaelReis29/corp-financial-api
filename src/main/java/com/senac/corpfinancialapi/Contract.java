package com.senac.corpfinancialapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Contract {
    private @Id
    @GeneratedValue long id;

    private Long companyId;

    @NotBlank
    @Size(min = 3, max = 150)
    private String title;

    @Size(min = 3, max = 1000)
    private String description;

    private BigDecimal totalValue;

    @Size(min = 3, max = 10)
    private String currency;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(min = 3, max = 30)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Contract() {
    }

    public Contract(Long companyId, String title, String description, BigDecimal totalValue, String currency, LocalDate startDate, LocalDate endDate, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.totalValue = totalValue;
        this.currency = currency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return id == contract.id && Objects.equals(companyId, contract.companyId) && Objects.equals(title, contract.title) && Objects.equals(description, contract.description) && Objects.equals(totalValue, contract.totalValue) && Objects.equals(currency, contract.currency) && Objects.equals(startDate, contract.startDate) && Objects.equals(endDate, contract.endDate) && Objects.equals(status, contract.status) && Objects.equals(createdAt, contract.createdAt) && Objects.equals(updatedAt, contract.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyId, title, description, totalValue, currency, startDate, endDate, status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Contract{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", totalValue=" + totalValue +
                ", currency='" + currency + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
