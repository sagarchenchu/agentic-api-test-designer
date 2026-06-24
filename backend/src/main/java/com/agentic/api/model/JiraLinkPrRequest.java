package com.agentic.api.model;

import jakarta.validation.constraints.NotBlank;

public class JiraLinkPrRequest {

    @NotBlank(message = "jiraStoryKey is required")
    private String jiraStoryKey;

    @NotBlank(message = "prUrl is required")
    private String prUrl;

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }
}
