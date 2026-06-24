package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.agentic.api.exception.JiraNotAvailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JiraClientService {

    private final JiraProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public JiraClientService(JiraProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public boolean isAvailable() {
        return properties.isEnabled() && properties.isConfigured();
    }

    public void requireAvailable() {
        if (!properties.isEnabled()) {
            throw new JiraNotAvailableException(
                    "Jira integration is disabled. Set jira.enabled=true and configure JIRA_BASE_URL, JIRA_EMAIL, JIRA_API_TOKEN."
            );
        }
        if (!properties.isConfigured()) {
            throw new JiraNotAvailableException(
                    "Jira integration is enabled but not fully configured. Set JIRA_BASE_URL, JIRA_EMAIL, and JIRA_API_TOKEN."
            );
        }
    }

    public JsonNode fetchIssue(String issueKey) {
        requireAvailable();
        String url = properties.normalizedBaseUrl() + "/rest/api/3/issue/" + issueKey;
        try {
            String body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Jira API request failed with status " + ex.getStatusCode().value() + ": " + sanitize(ex.getResponseBodyAsString()),
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Jira API request failed: " + ex.getMessage(), ex);
        }
    }

    public void postComment(String issueKey, ObjectNode commentBody) {
        requireAvailable();
        String url = properties.normalizedBaseUrl() + "/rest/api/3/issue/" + issueKey + "/comment";
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .body(commentBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Jira comment request failed with status " + ex.getStatusCode().value() + ": " + sanitize(ex.getResponseBodyAsString()),
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Jira comment request failed: " + ex.getMessage(), ex);
        }
    }

    public String browseUrl(String issueKey) {
        return properties.normalizedBaseUrl() + "/browse/" + issueKey;
    }

    private String basicAuthHeader() {
        String credentials = properties.getEmail().trim() + ":" + properties.getApiToken();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String sanitize(String body) {
        if (body == null) {
            return "";
        }
        String redacted = body;
        String apiToken = properties.getApiToken();
        if (apiToken != null && !apiToken.isBlank()) {
            redacted = redacted.replace(apiToken, "[REDACTED]");
        }
        String email = properties.getEmail();
        if (email != null && !email.isBlank()) {
            redacted = redacted.replace(email, "[REDACTED_EMAIL]");
        }
        return redacted.length() > 500 ? redacted.substring(0, 500) : redacted;
    }
}
