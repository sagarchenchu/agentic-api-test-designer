package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.agentic.api.config.OpenAiProperties;
import com.agentic.api.config.SecurityProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SecretMaskingService {

    private static final String REDACTED = "[REDACTED]";
    private static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASIC_AUTH_PATTERN =
            Pattern.compile("Basic\\s+[A-Za-z0-9+/=]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTHORIZATION_HEADER_PATTERN =
            Pattern.compile("(?i)Authorization\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern API_KEY_QUERY_PATTERN =
            Pattern.compile("(?i)(api[_-]?key=)[^&\\s]+");
    private static final Pattern PASSWORD_LIKE_PATTERN =
            Pattern.compile("(?i)(password|passwd|pwd|secret|api[_-]?token)\\s*[:=]\\s*\\S+");

    private final OpenAiProperties openAiProperties;
    private final JiraProperties jiraProperties;
    private final SecurityProperties securityProperties;

    public SecretMaskingService(
            OpenAiProperties openAiProperties,
            JiraProperties jiraProperties,
            SecurityProperties securityProperties
    ) {
        this.openAiProperties = openAiProperties;
        this.jiraProperties = jiraProperties;
        this.securityProperties = securityProperties;
    }

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = value;
        for (String secret : knownSecrets()) {
            if (secret != null && !secret.isBlank()) {
                masked = masked.replace(secret, REDACTED);
            }
        }
        String email = jiraProperties.getEmail();
        if (email != null && !email.isBlank()) {
            masked = masked.replace(email, REDACTED_EMAIL);
        }
        masked = BEARER_PATTERN.matcher(masked).replaceAll("Bearer " + REDACTED);
        masked = BASIC_AUTH_PATTERN.matcher(masked).replaceAll("Basic " + REDACTED);
        masked = AUTHORIZATION_HEADER_PATTERN.matcher(masked).replaceAll("Authorization: " + REDACTED);
        masked = API_KEY_QUERY_PATTERN.matcher(masked).replaceAll("$1" + REDACTED);
        masked = PASSWORD_LIKE_PATTERN.matcher(masked).replaceAll("$1 " + REDACTED);
        return masked;
    }

    public List<String> maskList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> masked = new ArrayList<>();
        for (String value : values) {
            masked.add(mask(value));
        }
        return masked;
    }

    private List<String> knownSecrets() {
        List<String> secrets = new ArrayList<>();
        if (openAiProperties.getApiKey() != null && !openAiProperties.getApiKey().isBlank()) {
            secrets.add(openAiProperties.getApiKey());
        }
        if (jiraProperties.getApiToken() != null && !jiraProperties.getApiToken().isBlank()) {
            secrets.add(jiraProperties.getApiToken());
        }
        if (securityProperties.getApiToken() != null && !securityProperties.getApiToken().isBlank()) {
            secrets.add(securityProperties.getApiToken());
        }
        return secrets;
    }
}
