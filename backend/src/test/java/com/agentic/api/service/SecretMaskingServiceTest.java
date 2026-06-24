package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.agentic.api.config.OpenAiProperties;
import com.agentic.api.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretMaskingServiceTest {

    @Test
    void redactsConfiguredSecretsAndBearerTokens() {
        OpenAiProperties openAi = new OpenAiProperties();
        openAi.setApiKey("sk-openai-secret");
        JiraProperties jira = new JiraProperties();
        jira.setApiToken("jira-secret-token");
        jira.setEmail("qa@company.com");
        SecurityProperties security = new SecurityProperties();
        security.setApiToken("agentic-secret");

        SecretMaskingService service = new SecretMaskingService(openAi, jira, security);
        String masked = service.mask(
                "Bearer sk-openai-secret qa@company.com jira-secret-token agentic-secret Authorization: Basic abc"
        );

        assertFalse(masked.contains("sk-openai-secret"));
        assertFalse(masked.contains("jira-secret-token"));
        assertFalse(masked.contains("agentic-secret"));
        assertFalse(masked.contains("qa@company.com"));
        assertTrue(masked.contains("[REDACTED]"));
        assertTrue(masked.contains("[REDACTED_EMAIL]"));
    }
}
