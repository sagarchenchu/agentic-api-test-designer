package com.agentic.api.model;

import jakarta.validation.constraints.NotBlank;

public class JiraStoryRequest {

    @NotBlank(message = "jiraStoryKey is required")
    private String jiraStoryKey;

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }
}
