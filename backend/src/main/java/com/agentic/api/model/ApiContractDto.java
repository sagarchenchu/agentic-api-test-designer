package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class ApiContractDto {

    private String endpointPath;
    private String httpMethod;
    private String operationId;
    private String summary;
    private String description;
    private List<String> tags = new ArrayList<>();
    private List<ApiParameterDto> requiredHeaders = new ArrayList<>();
    private List<ApiParameterDto> pathParams = new ArrayList<>();
    private List<ApiParameterDto> queryParams = new ArrayList<>();
    private ApiRequestBodyDto requestBody;
    private List<ApiResponseDto> responses = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

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

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<ApiParameterDto> getRequiredHeaders() {
        return requiredHeaders;
    }

    public void setRequiredHeaders(List<ApiParameterDto> requiredHeaders) {
        this.requiredHeaders = requiredHeaders != null ? requiredHeaders : new ArrayList<>();
    }

    public List<ApiParameterDto> getPathParams() {
        return pathParams;
    }

    public void setPathParams(List<ApiParameterDto> pathParams) {
        this.pathParams = pathParams != null ? pathParams : new ArrayList<>();
    }

    public List<ApiParameterDto> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<ApiParameterDto> queryParams) {
        this.queryParams = queryParams != null ? queryParams : new ArrayList<>();
    }

    public ApiRequestBodyDto getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(ApiRequestBodyDto requestBody) {
        this.requestBody = requestBody;
    }

    public List<ApiResponseDto> getResponses() {
        return responses;
    }

    public void setResponses(List<ApiResponseDto> responses) {
        this.responses = responses != null ? responses : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}
