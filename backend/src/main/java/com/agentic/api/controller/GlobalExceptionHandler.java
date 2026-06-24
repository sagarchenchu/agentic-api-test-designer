package com.agentic.api.controller;

import com.agentic.api.exception.ContractNotFoundException;
import com.agentic.api.exception.JiraNotAvailableException;
import com.agentic.api.model.ApiErrorResponse;
import com.agentic.api.service.SecretMaskingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SecretMaskingService secretMaskingService;

    public GlobalExceptionHandler(SecretMaskingService secretMaskingService) {
        this.secretMaskingService = secretMaskingService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), secretMaskingService.mask(error.getDefaultMessage()));
        }
        String global = ex.getBindingResult().getGlobalErrors().stream()
                .findFirst()
                .map(error -> secretMaskingService.mask(error.getDefaultMessage()))
                .orElse("Validation failed");

        ApiErrorResponse body = ApiErrorResponse.of("Validation failed", global, "VALIDATION_ERROR");
        body.setDetails(fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleContractNotFound(ContractNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("Not found", secretMaskingService.mask(ex.getMessage()), "NOT_FOUND"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("Bad request", secretMaskingService.mask(ex.getMessage()), "BAD_REQUEST"));
    }

    @ExceptionHandler(JiraNotAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handleJiraNotAvailable(JiraNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of("Jira unavailable", secretMaskingService.mask(ex.getMessage()), "JIRA_UNAVAILABLE"));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("Not found", secretMaskingService.mask(ex.getMessage()), "NOT_FOUND"));
    }
}
