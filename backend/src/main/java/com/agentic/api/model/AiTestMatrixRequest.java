package com.agentic.api.model;

public class AiTestMatrixRequest {

    private String jiraStoryKey;
    private String jiraStoryText;
    private ApiContractDto apiContract;
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

    public String getFrameworkType() {
        return frameworkType;
    }

    public void setFrameworkType(String frameworkType) {
        this.frameworkType = frameworkType;
    }
}
