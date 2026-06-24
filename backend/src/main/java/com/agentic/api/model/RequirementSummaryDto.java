package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class RequirementSummaryDto {

    private String jiraKey;
    private String endpoint;
    private String method;
    private List<String> requiredHeaders = new ArrayList<>();
    private List<String> requestBodyFields = new ArrayList<>();
    private List<String> expectedStatusCodes = new ArrayList<>();
    private List<String> businessRules = new ArrayList<>();
    private List<String> assumptions = new ArrayList<>();

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getRequiredHeaders() {
        return requiredHeaders;
    }

    public void setRequiredHeaders(List<String> requiredHeaders) {
        this.requiredHeaders = requiredHeaders;
    }

    public List<String> getRequestBodyFields() {
        return requestBodyFields;
    }

    public void setRequestBodyFields(List<String> requestBodyFields) {
        this.requestBodyFields = requestBodyFields;
    }

    public List<String> getExpectedStatusCodes() {
        return expectedStatusCodes;
    }

    public void setExpectedStatusCodes(List<String> expectedStatusCodes) {
        this.expectedStatusCodes = expectedStatusCodes;
    }

    public List<String> getBusinessRules() {
        return businessRules;
    }

    public void setBusinessRules(List<String> businessRules) {
        this.businessRules = businessRules;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }
}
