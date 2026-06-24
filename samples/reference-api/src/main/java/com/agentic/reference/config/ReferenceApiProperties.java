package com.agentic.reference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "reference-api")
public class ReferenceApiProperties {

    private String validToken = "valid-token";
    private List<String> forbiddenClientIds = List.of("blocked-portal", "forbidden-client");
    private List<String> supportedCurrencies = List.of("USD", "EUR", "GBP");
    private String trigger500ClientId = "trigger-500";

    public String getValidToken() {
        return validToken;
    }

    public void setValidToken(String validToken) {
        this.validToken = validToken;
    }

    public List<String> getForbiddenClientIds() {
        return forbiddenClientIds;
    }

    public void setForbiddenClientIds(List<String> forbiddenClientIds) {
        this.forbiddenClientIds = forbiddenClientIds;
    }

    public List<String> getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(List<String> supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
    }

    public String getTrigger500ClientId() {
        return trigger500ClientId;
    }

    public void setTrigger500ClientId(String trigger500ClientId) {
        this.trigger500ClientId = trigger500ClientId;
    }
}
