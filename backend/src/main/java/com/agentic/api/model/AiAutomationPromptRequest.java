package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class AiAutomationPromptRequest {

    private String jiraStoryKey;
    private String jiraStoryText;
    private ApiContractDto apiContract;
    private List<TestCaseDto> testCases = new ArrayList<>();
    private String frameworkType;

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

    public ApiContractDto getApiContract() {
        return apiContract;
    }

    public void setApiContract(ApiContractDto apiContract) {
        this.apiContract = apiContract;
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }

    public String getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(String frameworkType) {
        this.frameworkType = frameworkType;
    }
}
