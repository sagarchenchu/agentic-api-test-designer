package com.agentic.api.model;

import jakarta.validation.constraints.NotBlank;

public class JiraLinkPrRequest {

    @NotBlank(message = "jiraStoryKey is required")
    private String jiraStoryKey;

    @NotBlank(message = "prUrl is required")
    private String prUrl;

    private String confirmation;

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

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }
}
