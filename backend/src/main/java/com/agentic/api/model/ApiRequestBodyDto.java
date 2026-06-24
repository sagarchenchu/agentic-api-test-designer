package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class ApiRequestBodyDto {

    private boolean required;
    private String contentType;
    private List<String> requiredFields = new ArrayList<>();
    private List<ApiFieldDto> fields = new ArrayList<>();

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields != null ? requiredFields : new ArrayList<>();
    }

    public List<ApiFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<ApiFieldDto> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }
}
