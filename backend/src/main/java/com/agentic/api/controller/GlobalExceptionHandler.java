package com.agentic.api.controller;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Object global = ex.getBindingResult().getGlobalErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(null);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation failed");
        body.put("fieldErrors", fieldErrors);
        if (global != null) {
            body.put("message", global);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
