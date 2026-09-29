package com.senac.corpfinancialapi;

public class ContactNotFoundException extends RuntimeException {

    ContactNotFoundException(long id) {
        super("Could not find contact with ID: " + id);
    }
}
