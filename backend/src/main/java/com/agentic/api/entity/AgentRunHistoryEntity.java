package com.agentic.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agent_run_history")
public class AgentRunHistoryEntity {

    @Id
    private String runId;

    private String jiraStoryKey;
    private String swaggerSourceType;
    private String testGenerationMode;
    private String frameworkType;
    private String executionMode;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;
    private Integer testCaseCount;
    private Integer generatedFileCount;
    private String fileWriteSummary;
    private String testExecutionStatus;
    private String gitPrUrl;
    private String jiraUpdateStatus;

    @Column(length = 4000)
    private String warningsJson;

    @Column(length = 4000)
    private String errorsJson;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }

    public String getSwaggerSourceType() {
        return swaggerSourceType;
    }

    public void setSwaggerSourceType(String swaggerSourceType) {
        this.swaggerSourceType = swaggerSourceType;
    }

    public String getTestGenerationMode() {
        return testGenerationMode;
    }

    public void setTestGenerationMode(String testGenerationMode) {
        this.testGenerationMode = testGenerationMode;
    }

    public String getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(String frameworkType) {
        this.frameworkType = frameworkType;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTestCaseCount() {
        return testCaseCount;
    }

    public void setTestCaseCount(Integer testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    public Integer getGeneratedFileCount() {
        return generatedFileCount;
    }

    public void setGeneratedFileCount(Integer generatedFileCount) {
        this.generatedFileCount = generatedFileCount;
    }

    public String getFileWriteSummary() {
        return fileWriteSummary;
    }

    public void setFileWriteSummary(String fileWriteSummary) {
        this.fileWriteSummary = fileWriteSummary;
    }

    public String getTestExecutionStatus() {
        return testExecutionStatus;
    }

    public void setTestExecutionStatus(String testExecutionStatus) {
        this.testExecutionStatus = testExecutionStatus;
    }

    public String getGitPrUrl() {
        return gitPrUrl;
    }

    public void setGitPrUrl(String gitPrUrl) {
        this.gitPrUrl = gitPrUrl;
    }

    public String getJiraUpdateStatus() {
        return jiraUpdateStatus;
    }

    public void setJiraUpdateStatus(String jiraUpdateStatus) {
        this.jiraUpdateStatus = jiraUpdateStatus;
    }

    public String getWarningsJson() {
        return warningsJson;
    }

    public void setWarningsJson(String warningsJson) {
        this.warningsJson = warningsJson;
    }

    public String getErrorsJson() {
        return errorsJson;
    }

    public void setErrorsJson(String errorsJson) {
        this.errorsJson = errorsJson;
    }
}
