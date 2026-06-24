package com.agentic.api.model;

import java.util.ArrayList;
import java.util.List;

public class GeneratedFilesDto {

    private List<GeneratedFileDto> files = new ArrayList<>();

    public GeneratedFilesDto() {
    }

    public GeneratedFilesDto(List<GeneratedFileDto> files) {
        this.files = files;
    }

    public List<GeneratedFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<GeneratedFileDto> files) {
        this.files = files;
    }
}
