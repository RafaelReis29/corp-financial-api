package com.senac.corpfinancialapi;

public class PaymentMethodNotFoundException extends RuntimeException {

    PaymentMethodNotFoundException(long id) {
        super("Could not find payment method with ID: " + id);
    }
}
