package com.agentic.api.service;

import com.agentic.api.model.JiraLinkPrRequest;
import com.agentic.api.model.JiraPostSummaryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JiraCommentBuilderTest {

    private JiraCommentBuilder commentBuilder;

    @BeforeEach
    void setUp() {
        commentBuilder = new JiraCommentBuilder(new ObjectMapper());
    }

    @Test
    void buildsAdfSummaryComment() {
        JiraPostSummaryRequest request = new JiraPostSummaryRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setTestCaseCount(12);
        request.setBddGenerated(true);
        request.setFilesWritten(7);
        request.setExecutionStatus("PASSED");
        request.setPassed(12);
        request.setFailed(0);
        request.setPrUrl("https://github.com/org/repo/pull/99");
        request.setSerenityReportPath("target/site/serenity/index.html");

        ObjectNode body = commentBuilder.buildSummaryComment(request);

        assertEquals("doc", body.path("body").path("type").asText());
        assertTrue(body.toString().contains("Agentic API Test Designer Summary"));
        assertTrue(body.toString().contains("https://github.com/org/repo/pull/99"));
    }

    @Test
    void buildsAdfLinkPrComment() {
        JiraLinkPrRequest request = new JiraLinkPrRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setPrUrl("https://github.com/org/repo/pull/99");

        ObjectNode body = commentBuilder.buildLinkPrComment(request);

        assertEquals("doc", body.path("body").path("type").asText());
        assertTrue(body.toString().contains("Pull Request Linked"));
        assertTrue(body.toString().contains("https://github.com/org/repo/pull/99"));
    }
}
