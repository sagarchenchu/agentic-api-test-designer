package com.agentic.api.service;

import com.agentic.api.config.JiraProperties;
import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class JiraStoryService {

    private final JiraProperties properties;
    private final JiraClientService jiraClientService;
    private final JiraKeyValidator jiraKeyValidator;
    private final JiraAdfTextExtractor adfTextExtractor;
    private final JiraCommentBuilder commentBuilder;
    private final OperationConfirmationService operationConfirmationService;
    private final RunHistoryService runHistoryService;
    private final SecretMaskingService secretMaskingService;

    public JiraStoryService(
            JiraProperties properties,
            JiraClientService jiraClientService,
            JiraKeyValidator jiraKeyValidator,
            JiraAdfTextExtractor adfTextExtractor,
            JiraCommentBuilder commentBuilder,
            OperationConfirmationService operationConfirmationService,
            RunHistoryService runHistoryService,
            SecretMaskingService secretMaskingService
    ) {
        this.properties = properties;
        this.jiraClientService = jiraClientService;
        this.jiraKeyValidator = jiraKeyValidator;
        this.adfTextExtractor = adfTextExtractor;
        this.commentBuilder = commentBuilder;
        this.operationConfirmationService = operationConfirmationService;
        this.runHistoryService = runHistoryService;
        this.secretMaskingService = secretMaskingService;
    }

    public JiraConfigStatusResponse getConfigStatus() {
        JiraConfigStatusResponse response = new JiraConfigStatusResponse();
        response.setEnabled(properties.isEnabled());
        response.setConfigured(properties.isConfigured());

        if (!properties.isEnabled()) {
            response.setMessage("Jira integration is disabled");
            return response;
        }
        if (!properties.isConfigured()) {
            response.setMessage("Jira is enabled but base URL, email, or API token is missing");
            return response;
        }

        response.setBaseUrl(properties.normalizedBaseUrl());
        response.setMessage("Jira integration is configured");
        return response;
    }

    public JiraStoryResponse fetchStory(JiraStoryRequest request) {
        String issueKey = jiraKeyValidator.normalizeAndValidate(request.getJiraStoryKey());
        jiraClientService.requireAvailable();

        JsonNode issue = jiraClientService.fetchIssue(issueKey);
        JsonNode fields = issue.path("fields");

        JiraStoryResponse response = new JiraStoryResponse();
        response.setJiraStoryKey(issueKey);
        response.setSummary(fields.path("summary").asText(""));
        response.setStatus(fields.path("status").path("name").asText(""));
        response.setIssueType(fields.path("issuetype").path("name").asText(""));
        response.setPriority(fields.path("priority").path("name").asText(""));

        String description = adfTextExtractor.adfToPlainText(fields.path("description"));
        response.setDescription(description);
        response.setAcceptanceCriteria(adfTextExtractor.extractAcceptanceCriteria(description));
        if (response.getAcceptanceCriteria().isEmpty()) {
            response.getWarnings().add("No acceptance criteria section found in Jira description");
        }

        response.setLabels(readStringArray(fields.path("labels")));
        response.setComponents(readComponentNames(fields.path("components")));
        response.setEpicKey(extractEpicKey(fields));
        response.setUrl(jiraClientService.browseUrl(issueKey));
        return response;
    }

    public JiraOperationResponse postSummary(JiraPostSummaryRequest request) {
        operationConfirmationService.requireConfirmation(request.getConfirmation());
        String issueKey = jiraKeyValidator.normalizeAndValidate(request.getJiraStoryKey());
        jiraClientService.requireAvailable();

        JiraOperationResponse response = new JiraOperationResponse();
        response.setJiraStoryKey(issueKey);
        response.setUrl(jiraClientService.browseUrl(issueKey));

        try {
            ObjectNode comment = commentBuilder.buildSummaryComment(request);
            jiraClientService.postComment(issueKey, comment);
            response.setStatus("SUCCESS");
            response.setMessage("Summary comment posted to Jira");
        } catch (Exception ex) {
            response.setStatus("FAILED");
            response.getErrors().add(ex.getMessage());
            response.setMessage("Failed to post summary to Jira");
        }
        maskOperation(response);
        runHistoryService.recordJiraOperation(response, "JIRA_SUMMARY");
        return response;
    }

    public JiraOperationResponse linkPr(JiraLinkPrRequest request) {
        operationConfirmationService.requireConfirmation(request.getConfirmation());
        String issueKey = jiraKeyValidator.normalizeAndValidate(request.getJiraStoryKey());
        jiraClientService.requireAvailable();

        JiraOperationResponse response = new JiraOperationResponse();
        response.setJiraStoryKey(issueKey);
        response.setUrl(jiraClientService.browseUrl(issueKey));

        try {
            ObjectNode comment = commentBuilder.buildLinkPrComment(request);
            jiraClientService.postComment(issueKey, comment);
            response.setStatus("SUCCESS");
            response.setMessage("Pull request link posted to Jira");
        } catch (Exception ex) {
            response.setStatus("FAILED");
            response.getErrors().add(ex.getMessage());
            response.setMessage("Failed to link pull request in Jira");
        }
        maskOperation(response);
        runHistoryService.recordJiraOperation(response, "JIRA_LINK_PR");
        return response;
    }

    private void maskOperation(JiraOperationResponse response) {
        response.setMessage(secretMaskingService.mask(response.getMessage()));
        response.setWarnings(secretMaskingService.maskList(response.getWarnings()));
        response.setErrors(secretMaskingService.maskList(response.getErrors()));
    }

    private List<String> readStringArray(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return values;
        }
        for (JsonNode item : arrayNode) {
            if (item.isTextual()) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private List<String> readComponentNames(JsonNode componentsNode) {
        List<String> names = new ArrayList<>();
        if (componentsNode == null || !componentsNode.isArray()) {
            return names;
        }
        for (JsonNode component : componentsNode) {
            String name = component.path("name").asText("");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private String extractEpicKey(JsonNode fields) {
        JsonNode parent = fields.path("parent");
        if (!parent.isMissingNode() && parent.has("key")) {
            return parent.path("key").asText(null);
        }

        Iterator<String> fieldNames = fields.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.startsWith("customfield_")) {
                continue;
            }
            JsonNode value = fields.path(fieldName);
            if (value.isTextual() && value.asText().matches("^[A-Z][A-Z0-9]+-\\d+$")) {
                return value.asText();
            }
            if (value.has("key")) {
                return value.path("key").asText(null);
            }
        }
        return null;
    }
}
