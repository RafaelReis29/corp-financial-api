package com.senac.corpfinancialapi;

import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Hidden
@RestControllerAdvice
class ApiProblemAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiProblemAdvice.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = invalidFields(exception.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldIssue(error instanceof FieldError field ? field.getField() : "body", error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidParameters(ConstraintViolationException exception) {
        return invalidFields(exception.getConstraintViolations().stream()
                .map(error -> new FieldIssue(error.getPropertyPath().toString(), error.getMessage()))
                .toList());
    }

    @ExceptionHandler({CompanyNotFoundException.class, ContactNotFoundException.class, ContractNotFoundException.class,
            InvoiceNotFoundException.class, PaymentMethodNotFoundException.class})
    ProblemDetail resourceNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail invalidSort() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Sort by a field that exists on this resource.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Check that unique values are unused, referenced records exist, and records to be deleted have no dependents.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpectedError(Exception exception) {
        LOGGER.error("Unexpected API failure", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private static ProblemDetail invalidFields(List<FieldIssue> issues) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Correct the invalid fields and retry.");
        problem.setProperty("errors", issues);
        return problem;
    }

    record FieldIssue(String field, String message) {
    }
}
