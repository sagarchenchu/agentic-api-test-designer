package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class GeneratedFilesDto {

    private List<GeneratedFileDto> files = new ArrayList<>();
    private GeneratedBddDto generatedBdd;

    public GeneratedFilesDto() {
    }

    public GeneratedFilesDto(List<GeneratedFileDto> files, GeneratedBddDto generatedBdd) {
        this.files = files;
        this.generatedBdd = generatedBdd;
    }

    public List<GeneratedFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<GeneratedFileDto> files) {
        this.files = files;
    }

    public GeneratedBddDto getGeneratedBdd() {
        return generatedBdd;
    }

    public void setGeneratedBdd(GeneratedBddDto generatedBdd) {
        this.generatedBdd = generatedBdd;
    }
}
