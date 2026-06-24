package com.agentic.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

public class AgentRequest {

    @NotBlank(message = "jiraStoryKey is required")
    private String jiraStoryKey;

    private String jiraStoryText;

    private String swaggerUrl;

    private String swaggerJson;

    @NotBlank(message = "baseApiUrl is required")
    private String baseApiUrl;

    @NotBlank(message = "endpointPath is required")
    private String endpointPath;

    @NotBlank(message = "httpMethod is required")
    @Pattern(
            regexp = "^(?i)(GET|POST|PUT|PATCH|DELETE)$",
            message = "httpMethod must be one of GET, POST, PUT, PATCH, DELETE"
    )
    private String httpMethod;

    @Valid
    private List<HeaderEntryDto> headers = new ArrayList<>();

    @Pattern(
            regexp = "^(?!.*(?i)(password|passwd|pwd|secret|api[_-]?key)).*$",
            message = "credentialRef must not contain password-like or secret-like values"
    )
    private String credentialRef;

    private String projectPath;

    private String executionMode;

    private String frameworkType;

    @AssertTrue(message = "Either swaggerUrl or swaggerJson is required")
    public boolean isSwaggerProvided() {
        boolean hasUrl = swaggerUrl != null && !swaggerUrl.isBlank();
        boolean hasJson = swaggerJson != null && !swaggerJson.isBlank();
        return hasUrl || hasJson;
    }

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }

    public String getJiraStoryText() {
        return jiraStoryText;
    }

    public void setJiraStoryText(String jiraStoryText) {
        this.jiraStoryText = jiraStoryText;
    }

    public String getSwaggerUrl() {
        return swaggerUrl;
    }

    public void setSwaggerUrl(String swaggerUrl) {
        this.swaggerUrl = swaggerUrl;
    }

    public String getSwaggerJson() {
        return swaggerJson;
    }

    public void setSwaggerJson(String swaggerJson) {
        this.swaggerJson = swaggerJson;
    }

    public String getBaseApiUrl() {
        return baseApiUrl;
    }

    public void setBaseApiUrl(String baseApiUrl) {
        this.baseApiUrl = baseApiUrl;
    }

    public String getEndpointPath() {
        return endpointPath;
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<HeaderEntryDto> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeaderEntryDto> headers) {
        this.headers = headers != null ? headers : new ArrayList<>();
    }

    public String getCredentialRef() {
        return credentialRef;
    }

    public void setCredentialRef(String credentialRef) {
        this.credentialRef = credentialRef;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(String frameworkType) {
        this.frameworkType = frameworkType;
    }
}
