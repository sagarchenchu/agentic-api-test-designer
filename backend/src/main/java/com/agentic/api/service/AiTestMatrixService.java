package com.agentic.api.service;

import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiTestMatrixService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "Positive", "Negative", "Boundary", "Security", "Schema", "Business"
    );
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("High", "Medium", "Low");

    private final OpenApiParserService openApiParserService;
    private final ContractTestMatrixService contractTestMatrixService;
    private final OpenAiClientService openAiClientService;
    private final ObjectMapper objectMapper;

    public AiTestMatrixService(
            OpenApiParserService openApiParserService,
            ContractTestMatrixService contractTestMatrixService,
            OpenAiClientService openAiClientService,
            ObjectMapper objectMapper
    ) {
        this.openApiParserService = openApiParserService;
        this.contractTestMatrixService = contractTestMatrixService;
        this.openAiClientService = openAiClientService;
        this.objectMapper = objectMapper;
    }

    public TestMatrixResponse generateAiTestMatrix(AgentRequest request) {
        ApiContractDto contract = openApiParserService.extractContract(request);

        if (!openAiClientService.isEnabled()) {
            TestMatrixResponse fallback = contractTestMatrixService.generateFromContract(contract);
            fallback.getWarnings().add("OpenAI not configured. Used deterministic Swagger rules.");
            return fallback;
        }

        try {
            TestMatrixResponse aiResponse = generateFromAi(request, contract);
            if (aiResponse.getTestCases().isEmpty()) {
                return deterministicFallback(contract, "AI returned no test cases. Used deterministic Swagger rules.");
            }
            return aiResponse;
        } catch (Exception ex) {
            return deterministicFallback(contract,
                    "AI generation failed: " + ex.getMessage() + ". Used deterministic Swagger rules.");
        }
    }

    TestMatrixResponse generateFromAi(AgentRequest request, ApiContractDto contract) throws Exception {
        AiTestMatrixRequest promptPayload = new AiTestMatrixRequest();
        promptPayload.setJiraStoryKey(request.getJiraStoryKey());
        promptPayload.setJiraStoryText(request.getJiraStoryText());
        promptPayload.setApiContract(contract);
        promptPayload.setFrameworkType(request.getFrameworkType());

        String systemPrompt = """
                You are an expert API QA engineer. Generate API test cases using Jira story context and a structured API contract.
                Return strict JSON only with this schema:
                {
                  "testCases": [
                    {
                      "id": "TC_001",
                      "scenarioName": "",
                      "type": "Positive|Negative|Boundary|Security|Schema|Business",
                      "inputVariation": "",
                      "expectedStatus": "",
                      "expectedValidation": "",
                      "priority": "High|Medium|Low",
                      "automationStatus": "Ready",
                      "source": "JIRA|SWAGGER|JIRA+SWAGGER"
                    }
                  ],
                  "warnings": [],
                  "assumptions": []
                }
                Include positive, negative, required field, invalid type, invalid enum, boundary, security/auth, business rule,
                schema validation, and error response tests where applicable. Use Jira acceptance criteria for business rules.
                Do not include markdown or commentary outside JSON.
                """;

        String userPrompt = objectMapper.writeValueAsString(promptPayload);
        String aiJson = openAiClientService.completeJson(systemPrompt, userPrompt);
        return parseAiResponse(aiJson);
    }

    TestMatrixResponse parseAiResponse(String aiJson) throws Exception {
        String cleaned = stripCodeFences(aiJson);
        JsonNode root = objectMapper.readTree(cleaned);

        TestMatrixResponse response = new TestMatrixResponse();
        JsonNode casesNode = root.path("testCases");
        if (!casesNode.isArray()) {
            throw new IllegalStateException("AI response missing testCases array");
        }

        List<TestCaseDto> testCases = new ArrayList<>();
        int index = 1;
        for (JsonNode node : casesNode) {
            TestCaseDto testCase = new TestCaseDto();
            testCase.setId(textOrDefault(node, "id", String.format("TC_%03d", index++)));
            testCase.setScenarioName(requiredText(node, "scenarioName"));
            testCase.setType(normalizeType(textOrDefault(node, "type", "Positive")));
            testCase.setInputVariation(textOrDefault(node, "inputVariation", ""));
            testCase.setExpectedStatus(textOrDefault(node, "expectedStatus", "400"));
            testCase.setExpectedValidation(textOrDefault(node, "expectedValidation", ""));
            testCase.setPriority(normalizePriority(textOrDefault(node, "priority", "Medium")));
            testCase.setAutomationStatus(textOrDefault(node, "automationStatus", "Ready"));
            testCase.setSource(textOrDefault(node, "source", "JIRA+SWAGGER"));
            testCases.add(testCase);
        }
        response.setTestCases(testCases);
        response.setWarnings(readStringArray(root.path("warnings")));
        response.setAssumptions(readStringArray(root.path("assumptions")));
        return response;
    }

    private TestMatrixResponse deterministicFallback(ApiContractDto contract, String warning) {
        TestMatrixResponse fallback = contractTestMatrixService.generateFromContract(contract);
        fallback.getWarnings().add(warning);
        return fallback;
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
            throw new IllegalStateException("AI test case missing " + field);
        }
        return value;
    }

    private String normalizeType(String type) {
        if (ALLOWED_TYPES.contains(type)) {
            return type;
        }
        return "Business";
    }

    private String normalizePriority(String priority) {
        if (ALLOWED_PRIORITIES.contains(priority)) {
            return priority;
        }
        return "Medium";
    }
}
