package com.agentic.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JiraAdfTextExtractorTest {

    private JiraAdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JiraAdfTextExtractor(new ObjectMapper());
    }

    @Test
    void convertsAdfDescriptionToPlainText() throws Exception {
        String json = new String(
                getClass().getResourceAsStream("/sample-jira-issue.json").readAllBytes(),
                StandardCharsets.UTF_8
        );
        String description = extractor.adfToPlainText(
                new ObjectMapper().readTree(json).path("fields").path("description")
        );

        assertTrue(description.contains("As a payer"));
        assertTrue(description.contains("Acceptance Criteria"));
    }

    @Test
    void extractsAcceptanceCriteriaFromPlainTextSection() {
        String text = """
                Description line

                Acceptance Criteria
                - Valid request creates payment
                - Missing accountId returns 400
                """;

        assertEquals(
                java.util.List.of("Valid request creates payment", "Missing accountId returns 400"),
                extractor.extractAcceptanceCriteria(text)
        );
    }

    @Test
    void returnsEmptyCriteriaWithNoSection() {
        assertTrue(extractor.extractAcceptanceCriteria("No criteria here").isEmpty());
    }
}
