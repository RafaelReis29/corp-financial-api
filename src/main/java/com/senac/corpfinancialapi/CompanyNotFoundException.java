package com.senac.corpfinancialapi;

public class CompanyNotFoundException extends RuntimeException {

    CompanyNotFoundException(long id) {
        super("Could not find company with ID: " + id);
    }
}
