package com.senac.corpfinancialapi;

public class ContractNotFoundException extends RuntimeException {

    ContractNotFoundException(long id) {
        super("Could not find contract with ID: " + id);
    }
}
