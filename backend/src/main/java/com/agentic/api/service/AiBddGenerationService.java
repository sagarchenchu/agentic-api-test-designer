package com.agentic.api.service;

import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiBddGenerationService {

    private final OpenAiClientService openAiClientService;
    private final DeterministicAutomationService deterministicAutomationService;
    private final ObjectMapper objectMapper;

    public AiBddGenerationService(
            OpenAiClientService openAiClientService,
            DeterministicAutomationService deterministicAutomationService,
            ObjectMapper objectMapper
    ) {
        this.openAiClientService = openAiClientService;
        this.deterministicAutomationService = deterministicAutomationService;
        this.objectMapper = objectMapper;
    }

    public BddGenerationResult generateBdd(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases
    ) {
        if (!openAiClientService.isEnabled()) {
            return fallback(request, contract, testCases,
                    "OpenAI not configured. Used deterministic automation scaffold.");
        }

        try {
            BddGenerationResult aiResult = generateFromAi(request, contract, testCases);
            if (aiResult.bdd() == null || aiResult.bdd().getContent() == null || aiResult.bdd().getContent().isBlank()) {
                return fallback(request, contract, testCases,
                        "AI returned empty BDD content. Used deterministic automation scaffold.");
            }
            return aiResult;
        } catch (Exception ex) {
            return fallback(request, contract, testCases,
                    "AI BDD generation failed: " + ex.getMessage() + ". Used deterministic automation scaffold.");
        }
    }

    BddGenerationResult generateFromAi(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases
    ) throws Exception {
        AiAutomationPromptRequest promptPayload = buildPromptPayload(request, contract, testCases);

        String systemPrompt = """
                You are an expert API test automation engineer. Generate a Cucumber Gherkin feature file for RestAssured + Cucumber + Serenity.
                Return strict JSON only with this schema:
                {
                  "content": "...gherkin feature file...",
                  "downloadFilename": "payments.feature",
                  "warnings": [],
                  "assumptions": []
                }
                Rules:
                - Use Jira story key as a tag on the Feature line
                - Use each test case id as a scenario tag (e.g. @TC_001)
                - Use scenario names from the provided test cases
                - Use endpoint path and HTTP method from the API contract
                - Use generic reusable step wording (Given/When/Then)
                - Include Background with base URL configured and Authorization header available
                - Do NOT hardcode environment URLs, passwords, or tokens
                - Do NOT include implementation-specific logic inside Gherkin
                - Do not include markdown or commentary outside JSON
                """;

        String userPrompt = objectMapper.writeValueAsString(promptPayload);
        String aiJson = openAiClientService.completeJson(systemPrompt, userPrompt);
        return parseAiResponse(aiJson);
    }

    BddGenerationResult parseAiResponse(String aiJson) throws Exception {
        String cleaned = stripCodeFences(aiJson);
        JsonNode root = objectMapper.readTree(cleaned);

        String content = requiredText(root, "content");
        String downloadFilename = textOrDefault(root, "downloadFilename", "feature.feature");

        GeneratedBddDto bdd = new GeneratedBddDto(content, downloadFilename);
        List<String> warnings = readStringArray(root.path("warnings"));
        List<String> assumptions = readStringArray(root.path("assumptions"));
        return new BddGenerationResult(bdd, warnings, assumptions, false);
    }

    private BddGenerationResult fallback(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases,
            String warning
    ) {
        GeneratedBddDto bdd = deterministicAutomationService.generateBdd(request, contract, testCases);
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        return new BddGenerationResult(bdd, warnings, new ArrayList<>(), true);
    }

    private AiAutomationPromptRequest buildPromptPayload(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases
    ) {
        AiAutomationPromptRequest payload = new AiAutomationPromptRequest();
        payload.setJiraStoryKey(request.getJiraStoryKey());
        payload.setJiraStoryText(request.getJiraStoryText());
        payload.setApiContract(contract);
        payload.setTestCases(testCases);
        payload.setFrameworkType(request.getFrameworkType());
        return payload;
    }

    private String stripCodeFences(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return defaultValue;
        }
        return value.asText();
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrDefault(node, field, "");
        if (value.isBlank()) {
            throw new IllegalStateException("AI BDD response missing " + field);
        }
        return value;
    }

    public record BddGenerationResult(
            GeneratedBddDto bdd,
            List<String> warnings,
            List<String> assumptions,
            boolean fallbackUsed
    ) {
    }
}
