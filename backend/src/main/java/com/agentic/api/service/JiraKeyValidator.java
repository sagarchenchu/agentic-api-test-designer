package com.agentic.api.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class JiraKeyValidator {

    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]+-\\d+$");
    private static final Pattern UNSAFE_CHARS = Pattern.compile(
            "(?i)(\\s|;|&&|\\|\\||[|><`$]|\\.\\.|~|\\^|:|\\?|\\*|\\[|\\\\|/)"
    );

    public String normalizeAndValidate(String jiraStoryKey) {
        if (jiraStoryKey == null || jiraStoryKey.isBlank()) {
            throw new IllegalArgumentException("jiraStoryKey is required");
        }
        String trimmed = jiraStoryKey.trim();
        if (UNSAFE_CHARS.matcher(trimmed).find()) {
            throw new IllegalArgumentException("jiraStoryKey contains unsafe characters");
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!JIRA_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "jiraStoryKey must match pattern ^[A-Z][A-Z0-9]+-\\d+$ (example: PAY-1234)"
            );
        }
        return normalized;
    }
}
