package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class RunHistoryDetailDto extends RunHistorySummaryDto {

    private String swaggerSourceType;
    private List<RunArtifactDto> artifacts = new ArrayList<>();
    private List<ExternalOperationDto> externalOperations = new ArrayList<>();

    public String getSwaggerSourceType() {
        return swaggerSourceType;
    }

    public void setSwaggerSourceType(String swaggerSourceType) {
        this.swaggerSourceType = swaggerSourceType;
    }

    public List<RunArtifactDto> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<RunArtifactDto> artifacts) {
        this.artifacts = artifacts != null ? artifacts : new ArrayList<>();
    }

    public List<ExternalOperationDto> getExternalOperations() {
        return externalOperations;
    }

    public void setExternalOperations(List<ExternalOperationDto> externalOperations) {
        this.externalOperations = externalOperations != null ? externalOperations : new ArrayList<>();
    }
}
