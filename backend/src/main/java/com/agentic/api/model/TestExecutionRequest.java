package com.agentic.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TestExecutionRequest {

    @NotBlank(message = "projectPath is required")
    private String projectPath;

    private String commandType = "MAVEN";

    private String mavenCommand = "mvn clean verify";

    private String testTag;

    private String profile = "qa";

    @Min(value = 30, message = "timeoutSeconds must be at least 30")
    @Max(value = 900, message = "timeoutSeconds must be at most 900")
    private int timeoutSeconds = 300;

    private String environment = "QA";

    private boolean dryRun = false;

    private String confirmation;

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getMavenCommand() {
        return mavenCommand;
    }

    public void setMavenCommand(String mavenCommand) {
        this.mavenCommand = mavenCommand;
    }

    public String getTestTag() {
        return testTag;
    }

    public void setTestTag(String testTag) {
        this.testTag = testTag;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
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
