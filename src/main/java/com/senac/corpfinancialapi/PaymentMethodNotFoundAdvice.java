package com.senac.corpfinancialapi;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentMethodNotFoundAdvice {

    @Hidden
    @ExceptionHandler(PaymentMethodNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String paymentMethodNotFoundHandler(PaymentMethodNotFoundException ex) {
        return ex.getMessage();
    }
}
