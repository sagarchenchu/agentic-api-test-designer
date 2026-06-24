package com.agentic.api.model;

public class GeneratedFileDto {

    private String path;
    private String content;
    private String language;

    public GeneratedFileDto() {
    }

    public GeneratedFileDto(String path, String content, String language) {
        this.path = path;
        this.content = content;
        this.language = language;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
