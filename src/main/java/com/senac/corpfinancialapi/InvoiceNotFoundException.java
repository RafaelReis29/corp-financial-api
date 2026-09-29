package com.senac.corpfinancialapi;

public class InvoiceNotFoundException extends RuntimeException {

    InvoiceNotFoundException(long id) {
        super("Could not find invoice with ID: " + id);
    }
}
