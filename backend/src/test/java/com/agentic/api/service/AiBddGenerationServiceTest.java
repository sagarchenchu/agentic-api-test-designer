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
class AiBddGenerationServiceTest {

    @Mock
    private OpenAiClientService openAiClientService;

    private DeterministicAutomationService deterministicAutomationService;
    private AiBddGenerationService aiBddGenerationService;
    private ApiContractDto contract;
    private List<TestCaseDto> testCases;
    private String sampleAiBdd;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        deterministicAutomationService = new DeterministicAutomationService();
        aiBddGenerationService = new AiBddGenerationService(
                openAiClientService,
                deterministicAutomationService,
                objectMapper
        );
        sampleAiBdd = new ClassPathResource("sample-ai-bdd.json")
                .getContentAsString(StandardCharsets.UTF_8);
        contract = sampleContract();
        testCases = sampleTestCases();
    }

    @Test
    void fallsBackWhenOpenAiDisabled() {
        when(openAiClientService.isEnabled()).thenReturn(false);

        AiBddGenerationService.BddGenerationResult result = aiBddGenerationService.generateBdd(
                validRequest(), contract, testCases
        );

        assertNotNull(result.bdd().getContent());
        assertTrue(result.bdd().getContent().contains("@PAY-1234"));
        assertTrue(result.bdd().getContent().contains("@TC_001"));
        assertTrue(result.fallbackUsed());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.contains("OpenAI not configured")));
        verify(openAiClientService, never()).completeJson(anyString(), anyString());
    }

    @Test
    void usesMockedAiResponse() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(sampleAiBdd);

        AiBddGenerationService.BddGenerationResult result = aiBddGenerationService.generateBdd(
                validRequest(), contract, testCases
        );

        assertTrue(result.bdd().getContent().contains("Create payment with valid request"));
        assertEquals("payment.feature", result.bdd().getDownloadFilename());
        assertFalse(result.fallbackUsed());
        assertEquals(1, result.assumptions().size());
    }

    @Test
    void fallsBackWhenAiJsonInvalid() {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn("{ not-json");

        AiBddGenerationService.BddGenerationResult result = aiBddGenerationService.generateBdd(
                validRequest(), contract, testCases
        );

        assertNotNull(result.bdd().getContent());
        assertTrue(result.fallbackUsed());
        assertTrue(result.warnings().stream()
                .anyMatch(w -> w.contains("AI BDD generation failed")));
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
        ApiRequestBodyDto body = new ApiRequestBodyDto();
        body.setRequiredFields(List.of("accountId", "amount", "currency"));
        body.setFields(List.of(
                field("accountId", "string"),
                field("amount", "number"),
                field("currency", "string")
        ));
        contract.setRequestBody(body);
        return contract;
    }

    private ApiFieldDto field(String name, String type) {
        ApiFieldDto field = new ApiFieldDto();
        field.setName(name);
        field.setType(type);
        return field;
    }

    private List<TestCaseDto> sampleTestCases() {
        return List.of(
                new TestCaseDto("TC_001", "Create payment with valid request", "Positive",
                        "Valid body", "201", "Payment is created", "High", "Ready", "JIRA+SWAGGER"),
                new TestCaseDto("TC_002", "Missing required accountId", "Negative",
                        "accountId missing", "400", "Validation error", "High", "Ready", "SWAGGER")
        );
    }
}
