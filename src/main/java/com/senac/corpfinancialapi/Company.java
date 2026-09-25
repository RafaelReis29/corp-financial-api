package com.senac.corpfinancialapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Company {
    private @Id
    @GeneratedValue long id;

    @NotBlank
    @Size(min = 3, max = 100)
    private String tradeName;

    @Size(min = 3, max = 200)
    private String legalName;

    @NotBlank
    @Size(min = 3, max = 30)
    private String docNumber;

    @Size(min = 3, max = 100)
    private String sector;

    @Size(min = 3, max = 150)
    private String email;

    @Size(min = 3, max = 30)
    private String phone;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Company() {
    }

    public Company(String tradeName, String legalName, String docNumber, String sector, String email, String phone, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.tradeName = tradeName;
        this.legalName = legalName;
        this.docNumber = docNumber;
        this.sector = sector;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
        Company company = (Company) o;
        return id == company.id && Objects.equals(tradeName, company.tradeName) && Objects.equals(legalName, company.legalName) && Objects.equals(docNumber, company.docNumber) && Objects.equals(sector, company.sector) && Objects.equals(email, company.email) && Objects.equals(phone, company.phone) && Objects.equals(active, company.active) && Objects.equals(createdAt, company.createdAt) && Objects.equals(updatedAt, company.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tradeName, legalName, docNumber, sector, email, phone, active, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", tradeName='" + tradeName + '\'' +
                ", legalName='" + legalName + '\'' +
                ", docNumber='" + docNumber + '\'' +
                ", sector='" + sector + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
