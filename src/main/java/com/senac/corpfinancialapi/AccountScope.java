package com.senac.corpfinancialapi;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class AccountScope {

    static final String REQUEST_ACCOUNT = AccountScope.class.getName() + ".account";

    private final HttpServletRequest request;
    private final CompanyRepository companies;
    private final ContractRepository contracts;
    private final PaymentMethodRepository paymentMethods;

    AccountScope(HttpServletRequest request, CompanyRepository companies, ContractRepository contracts,
                 PaymentMethodRepository paymentMethods) {
        this.request = request;
        this.companies = companies;
        this.contracts = contracts;
        this.paymentMethods = paymentMethods;
    }

    Account account() {
        return (Account) request.getAttribute(REQUEST_ACCOUNT);
    }

    UUID accountId() {
        return account().id();
    }

    void requireCompany(Long companyId) {
        if (!companies.existsByIdAndAccount_Id(companyId, accountId())) {
            throw unavailableReference("companyId");
        }
    }

    void requireInvoiceReferences(Long contractId, Long paymentMethodId) {
        if (!contracts.existsByIdAndCompany_Account_Id(contractId, accountId())) {
            throw unavailableReference("contractId");
        }
        if (paymentMethodId != null && !paymentMethods.existsByIdAndCompany_Account_Id(paymentMethodId, accountId())) {
            throw unavailableReference("paymentMethodId");
        }
    }

    private static ResponseStatusException unavailableReference(String field) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Informe um " + field + " disponível nesta conta.");
    }
}
