package com.agentic.api.model;

public class TimelineStepDto {

    private String id;
    private String label;
    private String status;

    public TimelineStepDto() {
    }

    public TimelineStepDto(String id, String label, String status) {
        this.id = id;
        this.label = label;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
