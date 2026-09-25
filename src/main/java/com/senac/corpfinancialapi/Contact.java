package com.senac.corpfinancialapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Contact {
    private @Id
    @GeneratedValue long id;

    private Long companyId;

    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @Size(min = 3, max = 100)
    private String role;

    @Size(min = 3, max = 150)
    private String email;

    @Size(min = 3, max = 30)
    private String phone;

    private Boolean isPrimary;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Contact() {
    }

    public Contact(Long companyId, String name, String role, String email, String phone, Boolean isPrimary, Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.companyId = companyId;
        this.name = name;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.isPrimary = isPrimary;
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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
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
        Contact contact = (Contact) o;
        return id == contact.id && Objects.equals(companyId, contact.companyId) && Objects.equals(name, contact.name) && Objects.equals(role, contact.role) && Objects.equals(email, contact.email) && Objects.equals(phone, contact.phone) && Objects.equals(isPrimary, contact.isPrimary) && Objects.equals(active, contact.active) && Objects.equals(createdAt, contact.createdAt) && Objects.equals(updatedAt, contact.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyId, name, role, email, phone, isPrimary, active, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", isPrimary=" + isPrimary +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
