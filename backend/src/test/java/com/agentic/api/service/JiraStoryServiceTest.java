package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JiraStoryServiceTest {

    @Mock
    private JiraClientService jiraClientService;

    private JiraStoryService jiraStoryService;
    private JiraProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new JiraProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://company.atlassian.net");
        properties.setEmail("qa@company.com");
        properties.setApiToken("secret-token");

        objectMapper = new ObjectMapper();
        JiraAdfTextExtractor adfTextExtractor = new JiraAdfTextExtractor(objectMapper);
        jiraStoryService = new JiraStoryService(
                properties,
                jiraClientService,
                new JiraKeyValidator(),
                adfTextExtractor,
                new JiraCommentBuilder(objectMapper)
        );
    }

    @Test
    void configStatusDisabledByDefault() {
        JiraProperties disabled = new JiraProperties();
        JiraStoryService service = new JiraStoryService(
                disabled,
                jiraClientService,
                new JiraKeyValidator(),
                new JiraAdfTextExtractor(objectMapper),
                new JiraCommentBuilder(objectMapper)
        );

        JiraConfigStatusResponse status = service.getConfigStatus();

        assertFalse(status.isEnabled());
        assertFalse(status.isConfigured());
        assertTrue(status.getMessage().contains("disabled"));
    }

    @Test
    void mapsIssueJsonToStoryResponse() throws Exception {
        JsonNode issue = objectMapper.readTree(
                new String(getClass().getResourceAsStream("/sample-jira-issue.json").readAllBytes(), StandardCharsets.UTF_8)
        );
        when(jiraClientService.fetchIssue("PAY-1234")).thenReturn(issue);
        when(jiraClientService.browseUrl("PAY-1234"))
                .thenReturn("https://company.atlassian.net/browse/PAY-1234");

        JiraStoryRequest request = new JiraStoryRequest();
        request.setJiraStoryKey("pay-1234");

        JiraStoryResponse response = jiraStoryService.fetchStory(request);

        assertEquals("PAY-1234", response.getJiraStoryKey());
        assertEquals("Create payment API", response.getSummary());
        assertEquals("In Progress", response.getStatus());
        assertEquals("Story", response.getIssueType());
        assertEquals("High", response.getPriority());
        assertEquals(java.util.List.of("api", "payment"), response.getLabels());
        assertEquals(java.util.List.of("Payments"), response.getComponents());
        assertEquals("PAY-1000", response.getEpicKey());
        assertFalse(response.getAcceptanceCriteria().isEmpty());
    }

    @Test
    void postSummaryUsesJiraClient() {
        when(jiraClientService.browseUrl("PAY-1234"))
                .thenReturn("https://company.atlassian.net/browse/PAY-1234");

        JiraPostSummaryRequest request = new JiraPostSummaryRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setTestCaseCount(5);
        request.setBddGenerated(true);
        request.setFilesWritten(3);
        request.setExecutionStatus("PASSED");
        request.setPassed(5);

        JiraOperationResponse response = jiraStoryService.postSummary(request);

        assertEquals("SUCCESS", response.getStatus());
        verify(jiraClientService).postComment(eq("PAY-1234"), any());
    }

    @Test
    void linkPrUsesJiraClient() {
        when(jiraClientService.browseUrl("PAY-1234"))
                .thenReturn("https://company.atlassian.net/browse/PAY-1234");

        JiraLinkPrRequest request = new JiraLinkPrRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setPrUrl("https://github.com/org/repo/pull/99");

        JiraOperationResponse response = jiraStoryService.linkPr(request);

        assertEquals("SUCCESS", response.getStatus());
        verify(jiraClientService).postComment(eq("PAY-1234"), any());
    }
}
