package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class ApiResponseDto {

    private String statusCode;
    private String description;
    private String contentType;
    private List<String> fields = new ArrayList<>();
    private List<String> requiredFields = new ArrayList<>();

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields != null ? requiredFields : new ArrayList<>();
    }
}
