package com.agentic.api.model;

public class GeneratedBddDto {

    private String content;
    private String downloadFilename;

    public GeneratedBddDto() {
    }

    public GeneratedBddDto(String content, String downloadFilename) {
        this.content = content;
        this.downloadFilename = downloadFilename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDownloadFilename() {
        return downloadFilename;
    }

    public void setDownloadFilename(String downloadFilename) {
        this.downloadFilename = downloadFilename;
    }
}
