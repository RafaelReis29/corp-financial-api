package com.senac.corpfinancialapi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
public class PaymentMethod {
    private @Id
    @GeneratedValue long id;

    private Long companyId;

    @NotBlank
    @Size(min = 3, max = 50)
    private String type;

    @Size(min = 3, max = 100)
    private String provider;

    @NotBlank
    @Size(min = 3, max = 100)
    private String label;

    @Size(min = 3, max = 1000)
    private String details;

    private Boolean isDefault;

    private Boolean active;

    public PaymentMethod() {
    }

    public PaymentMethod(Long companyId, String type, String provider, String label, String details, Boolean isDefault, Boolean active) {
        this.companyId = companyId;
        this.type = type;
        this.provider = provider;
        this.label = label;
        this.details = details;
        this.isDefault = isDefault;
        this.active = active;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod paymentMethod = (PaymentMethod) o;
        return id == paymentMethod.id && Objects.equals(companyId, paymentMethod.companyId) && Objects.equals(type, paymentMethod.type) && Objects.equals(provider, paymentMethod.provider) && Objects.equals(label, paymentMethod.label) && Objects.equals(details, paymentMethod.details) && Objects.equals(isDefault, paymentMethod.isDefault) && Objects.equals(active, paymentMethod.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, companyId, type, provider, label, details, isDefault, active);
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", type='" + type + '\'' +
                ", provider='" + provider + '\'' +
                ", label='" + label + '\'' +
                ", details='" + details + '\'' +
                ", isDefault=" + isDefault +
                ", active=" + active +
                '}';
    }
}
