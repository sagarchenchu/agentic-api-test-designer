package com.agentic.api.service;

import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAutomationFileGenerationServiceTest {

    @Mock
    private OpenAiClientService openAiClientService;

    private DeterministicAutomationService deterministicAutomationService;
    private AiAutomationFileGenerationService aiAutomationFileGenerationService;
    private ApiContractDto contract;
    private List<TestCaseDto> testCases;
    private GeneratedBddDto bdd;
    private String sampleAiFiles;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        deterministicAutomationService = new DeterministicAutomationService();
        aiAutomationFileGenerationService = new AiAutomationFileGenerationService(
                openAiClientService,
                deterministicAutomationService,
                objectMapper
        );
        sampleAiFiles = new ClassPathResource("sample-ai-files.json")
                .getContentAsString(StandardCharsets.UTF_8);
        contract = sampleContract();
        testCases = sampleTestCases();
        bdd = deterministicAutomationService.generateBdd(validRequest(), contract, testCases);
    }

    @Test
    void fallsBackWhenOpenAiDisabled() {
        when(openAiClientService.isEnabled()).thenReturn(false);

        AiAutomationFileGenerationService.FilesGenerationResult result =
                aiAutomationFileGenerationService.generateFiles(
                        validRequest(), contract, testCases, bdd
                );

        assertEquals(7, result.files().size());
        assertTrue(result.files().stream()
                .anyMatch(file -> file.getContent().contains("verifyResponseValidation")));
        assertTrue(result.fallbackUsed());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.contains("OpenAI not configured")));
        verify(openAiClientService, never()).completeJson(anyString(), anyString());
    }

    @Test
    void usesMockedAiResponse() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(sampleAiFiles);

        AiAutomationFileGenerationService.FilesGenerationResult result =
                aiAutomationFileGenerationService.generateFiles(
                        validRequest(), contract, testCases, bdd
                );

        assertEquals(3, result.files().size());
        assertTrue(result.files().get(0).getPath().contains("create_payment.feature"));
        assertFalse(result.fallbackUsed());
    }

    @Test
    void fallsBackWhenAiJsonInvalid() {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn("{ not-json");

        AiAutomationFileGenerationService.FilesGenerationResult result =
                aiAutomationFileGenerationService.generateFiles(
                        validRequest(), contract, testCases, bdd
                );

        assertEquals(7, result.files().size());
        assertTrue(result.fallbackUsed());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.contains("AI file generation failed")));
    }

    private AgentRequest validRequest() {
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setJiraStoryText("Create payment API");
        request.setSwaggerJson("{}");
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setFrameworkType("restassured-cucumber-serenity");
        return request;
    }

    private ApiContractDto sampleContract() {
        ApiContractDto contract = new ApiContractDto();
        contract.setEndpointPath("/api/payments");
        contract.setHttpMethod("POST");
        contract.setSummary("Create payment");
        return contract;
    }

    private List<TestCaseDto> sampleTestCases() {
        return List.of(
                new TestCaseDto("TC_001", "Create payment with valid request", "Positive",
                        "Valid body", "201", "Payment is created", "High", "Ready", "JIRA+SWAGGER")
        );
    }
}
