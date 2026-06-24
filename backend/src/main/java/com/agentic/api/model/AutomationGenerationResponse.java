package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class AutomationGenerationResponse {

    private GeneratedBddDto generatedBdd;
    private List<GeneratedFileDto> generatedFiles = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> assumptions = new ArrayList<>();
    private String source;
    private boolean fallbackUsed;

    public GeneratedBddDto getGeneratedBdd() {
        return generatedBdd;
    }

    public void setGeneratedBdd(GeneratedBddDto generatedBdd) {
        this.generatedBdd = generatedBdd;
    }

    public List<GeneratedFileDto> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<GeneratedFileDto> generatedFiles) {
        this.generatedFiles = generatedFiles != null ? generatedFiles : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions != null ? assumptions : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
}
