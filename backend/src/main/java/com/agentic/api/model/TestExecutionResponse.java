package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class TestExecutionResponse {

    private String executionId;
    private String status;
    private String projectPath;
    private String command;
    private String startedAt;
    private String completedAt;
    private long durationMs;
    private Integer exitCode;
    private TestExecutionSummary summary = new TestExecutionSummary();
    private TestReportPaths reportPaths = new TestReportPaths();
    private List<FailedScenarioDto> failedScenarios = new ArrayList<>();
    private String logTail;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public TestExecutionSummary getSummary() {
        return summary;
    }

    public void setSummary(TestExecutionSummary summary) {
        this.summary = summary;
    }

    public TestReportPaths getReportPaths() {
        return reportPaths;
    }

    public void setReportPaths(TestReportPaths reportPaths) {
        this.reportPaths = reportPaths;
    }

    public List<FailedScenarioDto> getFailedScenarios() {
        return failedScenarios;
    }

    public void setFailedScenarios(List<FailedScenarioDto> failedScenarios) {
        this.failedScenarios = failedScenarios != null ? failedScenarios : new ArrayList<>();
    }

    public String getLogTail() {
        return logTail;
    }

    public void setLogTail(String logTail) {
        this.logTail = logTail;
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
