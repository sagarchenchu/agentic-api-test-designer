package com.agentic.api.service;

import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AiAutomationFileGenerationService {

    private static final Set<String> ALLOWED_LANGUAGES = Set.of("java", "json", "gherkin");

    private final OpenAiClientService openAiClientService;
    private final DeterministicAutomationService deterministicAutomationService;
    private final ObjectMapper objectMapper;

    public AiAutomationFileGenerationService(
            OpenAiClientService openAiClientService,
            DeterministicAutomationService deterministicAutomationService,
            ObjectMapper objectMapper
    ) {
        this.openAiClientService = openAiClientService;
        this.deterministicAutomationService = deterministicAutomationService;
        this.objectMapper = objectMapper;
    }

    public FilesGenerationResult generateFiles(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases,
            GeneratedBddDto bdd
    ) {
        if (!openAiClientService.isEnabled()) {
            return fallback(request, contract, testCases, bdd,
                    "OpenAI not configured. Used deterministic automation scaffold.");
        }

        try {
            FilesGenerationResult aiResult = generateFromAi(request, contract, testCases);
            if (aiResult.files() == null || aiResult.files().isEmpty()) {
                return fallback(request, contract, testCases, bdd,
                        "AI returned no automation files. Used deterministic automation scaffold.");
            }
            return aiResult;
        } catch (Exception ex) {
            return fallback(request, contract, testCases, bdd,
                    "AI file generation failed: " + ex.getMessage() + ". Used deterministic automation scaffold.");
        }
    }

    FilesGenerationResult generateFromAi(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases
    ) throws Exception {
        AiAutomationPromptRequest promptPayload = buildPromptPayload(request, contract, testCases);

        String systemPrompt = """
                You are an expert API test automation engineer. Generate RestAssured + Cucumber + Serenity automation scaffold files.
                Return strict JSON only with this schema:
                {
                  "files": [
                    {
                      "path": "src/test/resources/features/payment/create_payment.feature",
                      "content": "...",
                      "language": "gherkin|java|json"
                    }
                  ],
                  "warnings": [],
                  "assumptions": []
                }
                Generate these file types for the API operation:
                - feature file under src/test/resources/features/{resource}/{operation}.feature
                - request template JSON under src/test/resources/templates/{resource}/{operation}_request.json
                - QA test data JSON under src/test/resources/testdata/qa/{resource}/{operation}_data.json
                - response schema JSON under src/test/resources/schemas/{resource}/{operation}_response_schema.json
                - step definitions under src/test/java/steps/{resource}/{Operation}Steps.java
                - API client under src/test/java/api/{resource}/{Operation}ApiClient.java
                - validator under src/test/java/validators/{resource}/{Operation}Validator.java
                Do not hardcode environment URLs, passwords, or tokens.
                Do not include markdown or commentary outside JSON.
                """;

        String userPrompt = objectMapper.writeValueAsString(promptPayload);
        String aiJson = openAiClientService.completeJson(systemPrompt, userPrompt);
        return parseAiResponse(aiJson);
    }

    FilesGenerationResult parseAiResponse(String aiJson) throws Exception {
        String cleaned = stripCodeFences(aiJson);
        JsonNode root = objectMapper.readTree(cleaned);

        JsonNode filesNode = root.path("files");
        if (!filesNode.isArray() || filesNode.isEmpty()) {
            throw new IllegalStateException("AI response missing files array");
        }

        List<GeneratedFileDto> files = new ArrayList<>();
        for (JsonNode node : filesNode) {
            String path = requiredText(node, "path");
            String content = requiredText(node, "content");
            String language = normalizeLanguage(textOrDefault(node, "language", "java"));
            files.add(new GeneratedFileDto(path, content, language));
        }

        List<String> warnings = readStringArray(root.path("warnings"));
        List<String> assumptions = readStringArray(root.path("assumptions"));
        return new FilesGenerationResult(files, warnings, assumptions, false);
    }

    private FilesGenerationResult fallback(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases,
            GeneratedBddDto bdd,
            String warning
    ) {
        List<GeneratedFileDto> files = deterministicAutomationService.generateFiles(
                request, contract, testCases, bdd
        );
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        return new FilesGenerationResult(files, warnings, new ArrayList<>(), true);
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

    private String normalizeLanguage(String language) {
        String normalized = language.toLowerCase();
        if (ALLOWED_LANGUAGES.contains(normalized)) {
            return normalized;
        }
        return "java";
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
            throw new IllegalStateException("AI files response missing " + field);
        }
        return value;
    }

    public record FilesGenerationResult(
            List<GeneratedFileDto> files,
            List<String> warnings,
            List<String> assumptions,
            boolean fallbackUsed
    ) {
    }
}
