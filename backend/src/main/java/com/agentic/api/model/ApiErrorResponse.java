package com.agentic.api.model;

import java.util.HashMap;
import java.util.Map;

public class ApiErrorResponse {

    private String error;
    private String message;
    private String code;
    private Map<String, Object> details = new HashMap<>();

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(String error, String message, String code) {
        this.error = error;
        this.message = message;
        this.code = code;
    }

    public static ApiErrorResponse of(String error, String message, String code) {
        return new ApiErrorResponse(error, message, code);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? details : new HashMap<>();
    }
}
