package com.agentic.api.model;

public class ExecutionReportDto {

    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private String duration;
    private String reportPath;
    private FailedScenarioDto failedScenario;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public FailedScenarioDto getFailedScenario() {
        return failedScenario;
    }

    public void setFailedScenario(FailedScenarioDto failedScenario) {
        this.failedScenario = failedScenario;
    }
}
