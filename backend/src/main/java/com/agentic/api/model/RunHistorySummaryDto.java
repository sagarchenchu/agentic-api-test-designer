package com.agentic.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RunHistorySummaryDto {

    private String runId;
    private String jiraStoryKey;
    private String status;
    private String executionMode;
    private String testGenerationMode;
    private String frameworkType;
    private Instant createdAt;
    private Instant updatedAt;
    private int testCaseCount;
    private int generatedFileCount;
    private String fileWriteSummary;
    private String testExecutionStatus;
    private String gitPrUrl;
    private String jiraUpdateStatus;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
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

    public int getTestCaseCount() {
        return testCaseCount;
    }

    public void setTestCaseCount(int testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    public int getGeneratedFileCount() {
        return generatedFileCount;
    }

    public void setGeneratedFileCount(int generatedFileCount) {
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

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
