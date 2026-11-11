package com.senac.corpfinancialapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Contract {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private @Id
    @GeneratedValue long id;

    @NotNull
    @Positive
    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;

    @NotBlank
    @Size(min = 3, max = 150)
    private String title;

    @Size(min = 3, max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @Column(precision = 19, scale = 2)
    private BigDecimal totalValue;

    @NotNull
    @Pattern(regexp = "[A-Z]{3}")
    private String currency;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull
    @Pattern(regexp = "ATIVO|RASCUNHO|SUSPENSO|ENCERRADO|CANCELADO")
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

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
