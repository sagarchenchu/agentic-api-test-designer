package com.agentic.api.model;

public class TestReportPaths {

    private String surefire;
    private String failsafe;
    private String serenity;
    private String cucumberJson;

    public String getSurefire() {
        return surefire;
    }

    public void setSurefire(String surefire) {
        this.surefire = surefire;
    }

    public String getFailsafe() {
        return failsafe;
    }

    public void setFailsafe(String failsafe) {
        this.failsafe = failsafe;
    }

    public String getSerenity() {
        return serenity;
    }

    public void setSerenity(String serenity) {
        this.serenity = serenity;
    }

    public String getCucumberJson() {
        return cucumberJson;
    }

    public void setCucumberJson(String cucumberJson) {
        this.cucumberJson = cucumberJson;
    }
}
