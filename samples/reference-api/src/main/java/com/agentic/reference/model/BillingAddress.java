package com.agentic.reference.model;

import jakarta.validation.constraints.NotBlank;

public class BillingAddress {

    @NotBlank
    private String line1;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @NotBlank
    private String zip;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }
}
