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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Invoice {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private @Id
    @GeneratedValue long id;

    @NotNull
    @Positive
    @Column(name = "contract_id")
    private Long contractId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    private Contract contract;

    @Size(min = 3, max = 500)
    @Column(length = 500)
    private String description;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate dueDate;

    private LocalDateTime paidAt;

    @NotNull
    @Pattern(regexp = "PENDENTE|PAGO|VENCIDO|ATRASADO|CANCELADO")
    private String status;

    @Positive
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", insertable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @JsonIgnore
    @AssertTrue(message = "paidAt is required for PAGO and must be absent for other statuses")
    public boolean isPaymentStatusValid() {
        return status == null || "PAGO".equals(status) == (paidAt != null);
    }

    public Invoice() {
    }

    public Invoice(Long contractId, String description, BigDecimal amount, LocalDate dueDate, LocalDateTime paidAt, String status, Long paymentMethodId) {
        this.contractId = contractId;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidAt = paidAt;
        this.status = status;
        this.paymentMethodId = paymentMethodId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return id == invoice.id && Objects.equals(contractId, invoice.contractId) && Objects.equals(description, invoice.description) && Objects.equals(amount, invoice.amount) && Objects.equals(dueDate, invoice.dueDate) && Objects.equals(paidAt, invoice.paidAt) && Objects.equals(status, invoice.status) && Objects.equals(paymentMethodId, invoice.paymentMethodId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contractId, description, amount, dueDate, paidAt, status, paymentMethodId);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", contractId=" + contractId +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", dueDate=" + dueDate +
                ", paidAt=" + paidAt +
                ", status='" + status + '\'' +
                ", paymentMethodId=" + paymentMethodId +
                '}';
    }
}
