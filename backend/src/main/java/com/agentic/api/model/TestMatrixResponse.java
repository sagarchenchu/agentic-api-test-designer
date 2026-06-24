package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class TestMatrixResponse {

    private List<TestCaseDto> testCases = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> assumptions = new ArrayList<>();

    public TestMatrixResponse() {
    }

    public TestMatrixResponse(List<TestCaseDto> testCases, List<String> warnings) {
        this.testCases = testCases;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions != null ? assumptions : new ArrayList<>();
    }
}
