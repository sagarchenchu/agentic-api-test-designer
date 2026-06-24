package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class FileWriteResponse {

    private String projectPath;
    private FileWriteSummary summary = new FileWriteSummary();
    private List<FileWriteResult> results = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public FileWriteSummary getSummary() {
        return summary;
    }

    public void setSummary(FileWriteSummary summary) {
        this.summary = summary;
    }

    public List<FileWriteResult> getResults() {
        return results;
    }

    public void setResults(List<FileWriteResult> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }
}
