package com.agentic.api.model;

public class TestCaseDto {

    private String id;
    private String scenarioName;
    private String type;
    private String inputVariation;
    private String expectedStatus;
    private String expectedValidation;
    private String priority;
    private String automationStatus;

    public TestCaseDto() {
    }

    public TestCaseDto(String id, String scenarioName, String type, String inputVariation,
                       String expectedStatus, String expectedValidation, String priority,
                       String automationStatus) {
        this.id = id;
        this.scenarioName = scenarioName;
        this.type = type;
        this.inputVariation = inputVariation;
        this.expectedStatus = expectedStatus;
        this.expectedValidation = expectedValidation;
        this.priority = priority;
        this.automationStatus = automationStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInputVariation() {
        return inputVariation;
    }

    public void setInputVariation(String inputVariation) {
        this.inputVariation = inputVariation;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getExpectedValidation() {
        return expectedValidation;
    }

    public void setExpectedValidation(String expectedValidation) {
        this.expectedValidation = expectedValidation;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAutomationStatus() {
        return automationStatus;
    }

    public void setAutomationStatus(String automationStatus) {
        this.automationStatus = automationStatus;
    }
}
