package com.agentic.api.service;

import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.TestMatrixResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTestMatrixServiceTest {

    @Mock
    private OpenAiClientService openAiClientService;

    private OpenApiParserService openApiParserService;
    private ContractTestMatrixService contractTestMatrixService;
    private AiTestMatrixService aiTestMatrixService;
    private String sampleSpec;
    private String sampleAiResponse;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        openApiParserService = new OpenApiParserService(objectMapper);
        contractTestMatrixService = new ContractTestMatrixService();
        aiTestMatrixService = new AiTestMatrixService(
                openApiParserService,
                contractTestMatrixService,
                openAiClientService,
                objectMapper
        );
        sampleSpec = new ClassPathResource("sample-openapi.json")
                .getContentAsString(StandardCharsets.UTF_8);
        sampleAiResponse = new ClassPathResource("sample-ai-test-matrix.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void fallsBackWhenOpenAiDisabled() {
        when(openAiClientService.isEnabled()).thenReturn(false);

        TestMatrixResponse response = aiTestMatrixService.generateAiTestMatrix(validRequest());

        assertFalse(response.getTestCases().isEmpty());
        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> w.contains("OpenAI not configured")));
        verify(openAiClientService, never()).completeJson(anyString(), anyString());
    }

    @Test
    void usesMockedAiResponse() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(sampleAiResponse);

        TestMatrixResponse response = aiTestMatrixService.generateAiTestMatrix(validRequest());

        assertEquals(2, response.getTestCases().size());
        assertEquals("JIRA", response.getTestCases().get(1).getSource());
        assertEquals(1, response.getAssumptions().size());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    void fallsBackWhenAiJsonInvalid() {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn("{ not-json");

        TestMatrixResponse response = aiTestMatrixService.generateAiTestMatrix(validRequest());

        assertFalse(response.getTestCases().isEmpty());
        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> w.contains("AI generation failed")));
    }

    @Test
    void fallsBackWhenAiReturnsNoTestCases() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString()))
                .thenReturn("{\"testCases\":[],\"warnings\":[],\"assumptions\":[]}");

        TestMatrixResponse response = aiTestMatrixService.generateAiTestMatrix(validRequest());

        assertFalse(response.getTestCases().isEmpty());
        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> w.contains("AI returned no test cases")));
    }

    private AgentRequest validRequest() {
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setJiraStoryText("Amount should not exceed daily limit of 5000");
        request.setSwaggerJson(sampleSpec);
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setFrameworkType("restassured-cucumber-serenity");
        return request;
    }
}
