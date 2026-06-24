package com.agentic.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class FileWriteRequest {

    @NotBlank(message = "projectPath is required")
    private String projectPath;

    @NotEmpty(message = "files must not be empty")
    @Valid
    private List<GeneratedFileDto> files = new ArrayList<>();

    /**
     * Informational only — the preview and write endpoints set this from the route.
     * Clients may omit it or send a hint for debugging; the server always enforces the correct mode.
     */
    private String writeMode = "preview";

    private boolean overwriteExisting = false;

    private boolean createBackup = true;

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public List<GeneratedFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<GeneratedFileDto> files) {
        this.files = files != null ? files : new ArrayList<>();
    }

    public String getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public boolean isCreateBackup() {
        return createBackup;
    }

    public void setCreateBackup(boolean createBackup) {
        this.createBackup = createBackup;
    }
}
