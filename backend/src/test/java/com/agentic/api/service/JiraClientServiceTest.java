package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class JiraClientServiceTest {

    @Test
    void sanitizeRedactsEmailAndTokenFromErrorBody() throws Exception {
        JiraProperties properties = new JiraProperties();
        properties.setEmail("qa@company.com");
        properties.setApiToken("secret-token-value");

        JiraClientService service = new JiraClientService(properties, new ObjectMapper());
        Method sanitize = JiraClientService.class.getDeclaredMethod("sanitize", String.class);
        sanitize.setAccessible(true);

        String result = (String) sanitize.invoke(
                service,
                "Auth failed for qa@company.com with token secret-token-value"
        );

        assertFalse(result.contains("qa@company.com"));
        assertFalse(result.contains("secret-token-value"));
        assertTrue(result.contains("[REDACTED_EMAIL]"));
        assertTrue(result.contains("[REDACTED]"));
    }
}
