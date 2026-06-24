package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class AgentRunResponse {

    private String runId;
    private String status;
    private RequirementSummaryDto requirementSummary;
    private List<TestCaseDto> testCases = new ArrayList<>();
    private GeneratedBddDto generatedBdd;
    private List<GeneratedFileDto> generatedFiles = new ArrayList<>();
    private ExecutionReportDto executionReport;
    private List<TimelineStepDto> timelineSteps = new ArrayList<>();
    private List<String> testMatrixWarnings = new ArrayList<>();
    private List<String> testMatrixAssumptions = new ArrayList<>();

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RequirementSummaryDto getRequirementSummary() {
        return requirementSummary;
    }

    public void setRequirementSummary(RequirementSummaryDto requirementSummary) {
        this.requirementSummary = requirementSummary;
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases;
    }

    public GeneratedBddDto getGeneratedBdd() {
        return generatedBdd;
    }

    public void setGeneratedBdd(GeneratedBddDto generatedBdd) {
        this.generatedBdd = generatedBdd;
    }

    public List<GeneratedFileDto> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<GeneratedFileDto> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public ExecutionReportDto getExecutionReport() {
        return executionReport;
    }

    public void setExecutionReport(ExecutionReportDto executionReport) {
        this.executionReport = executionReport;
    }

    public List<TimelineStepDto> getTimelineSteps() {
        return timelineSteps;
    }

    public void setTimelineSteps(List<TimelineStepDto> timelineSteps) {
        this.timelineSteps = timelineSteps;
    }

    public List<String> getTestMatrixWarnings() {
        return testMatrixWarnings;
    }

    public void setTestMatrixWarnings(List<String> testMatrixWarnings) {
        this.testMatrixWarnings = testMatrixWarnings != null ? testMatrixWarnings : new ArrayList<>();
    }

    public List<String> getTestMatrixAssumptions() {
        return testMatrixAssumptions;
    }

    public void setTestMatrixAssumptions(List<String> testMatrixAssumptions) {
        this.testMatrixAssumptions = testMatrixAssumptions != null ? testMatrixAssumptions : new ArrayList<>();
    }
}
