package com.agentic.api.model;

import jakarta.validation.constraints.NotBlank;

public class JiraPostSummaryRequest {

    @NotBlank(message = "jiraStoryKey is required")
    private String jiraStoryKey;

    private int testCaseCount;
    private boolean bddGenerated;
    private int filesWritten;
    private String executionStatus;
    private int passed;
    private int failed;
    private String prUrl;
    private String serenityReportPath;

    private String confirmation;

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }

    public int getTestCaseCount() {
        return testCaseCount;
    }

    public void setTestCaseCount(int testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    public boolean isBddGenerated() {
        return bddGenerated;
    }

    public void setBddGenerated(boolean bddGenerated) {
        this.bddGenerated = bddGenerated;
    }

    public int getFilesWritten() {
        return filesWritten;
    }

    public void setFilesWritten(int filesWritten) {
        this.filesWritten = filesWritten;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }

    public String getSerenityReportPath() {
        return serenityReportPath;
    }

    public void setSerenityReportPath(String serenityReportPath) {
        this.serenityReportPath = serenityReportPath;
    }

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }
}
