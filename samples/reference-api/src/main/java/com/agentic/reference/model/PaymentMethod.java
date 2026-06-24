package com.agentic.reference.model;

import jakarta.validation.constraints.NotBlank;

public class PaymentMethod {

    @NotBlank
    private String type;

    @NotBlank
    private String token;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
