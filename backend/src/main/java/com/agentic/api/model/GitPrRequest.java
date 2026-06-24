package com.agentic.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class GitPrRequest {

    @NotBlank(message = "projectPath is required")
    private String projectPath;

    private String jiraStoryKey;

    private String baseBranch = "main";

    private String newBranchName;

    private String commitMessage;

    private String prTitle;

    private String prBody;

    private String remoteName = "origin";

    @NotEmpty(message = "filesToCommit must not be empty")
    private List<String> filesToCommit = new ArrayList<>();

    private boolean dryRun = false;

    private String confirmation;

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getJiraStoryKey() {
        return jiraStoryKey;
    }

    public void setJiraStoryKey(String jiraStoryKey) {
        this.jiraStoryKey = jiraStoryKey;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getNewBranchName() {
        return newBranchName;
    }

    public void setNewBranchName(String newBranchName) {
        this.newBranchName = newBranchName;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public String getPrBody() {
        return prBody;
    }

    public void setPrBody(String prBody) {
        this.prBody = prBody;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public List<String> getFilesToCommit() {
        return filesToCommit;
    }

    public void setFilesToCommit(List<String> filesToCommit) {
        this.filesToCommit = filesToCommit != null ? filesToCommit : new ArrayList<>();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }
}
