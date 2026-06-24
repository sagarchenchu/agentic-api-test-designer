package com.agentic.api.service;

import com.agentic.api.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AutomationGenerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAiClientService openAiClientService;

    @Test
    void generateAiBddFallsBackWhenOpenAiDisabled() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/agent/generate-ai-bdd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(automationRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedBdd.content", containsString("@PAY-1234")))
                .andExpect(jsonPath("$.generatedBdd.content", containsString("@TC_001")))
                .andExpect(jsonPath("$.fallbackUsed", is(true)))
                .andExpect(jsonPath("$.warnings", not(empty())));
    }

    @Test
    void generateAiBddUsesMockedAiResponse() throws Exception {
        String sampleBdd = new ClassPathResource("sample-ai-bdd.json")
                .getContentAsString(StandardCharsets.UTF_8);
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(sampleBdd);

        mockMvc.perform(post("/api/agent/generate-ai-bdd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(automationRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedBdd.downloadFilename", is("payment.feature")))
                .andExpect(jsonPath("$.source", is("AI")))
                .andExpect(jsonPath("$.fallbackUsed", is(false)));
    }

    @Test
    void generateAiAutomationPackageReturnsBddAndFiles() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/agent/generate-ai-automation-package")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(automationRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedBdd.content", not(emptyString())))
                .andExpect(jsonPath("$.generatedFiles", hasSize(7)))
                .andExpect(jsonPath("$.generatedFiles[0].path", containsString("features")))
                .andExpect(jsonPath("$.fallbackUsed", is(true)));
    }

    @Test
    void generateAiFilesFallsBackOnInvalidAiJson() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn("{ invalid");

        mockMvc.perform(post("/api/agent/generate-ai-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(automationRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedFiles", hasSize(7)))
                .andExpect(jsonPath("$.fallbackUsed", is(true)))
                .andExpect(jsonPath("$.warnings", not(empty())));
    }

    private AutomationGenerationRequest automationRequest() throws Exception {
        String sampleSpec = new ClassPathResource("sample-openapi.json")
                .getContentAsString(StandardCharsets.UTF_8);

        AgentRequest agentRequest = new AgentRequest();
        agentRequest.setJiraStoryKey("PAY-1234");
        agentRequest.setJiraStoryText("Create payment with valid account");
        agentRequest.setSwaggerJson(sampleSpec);
        agentRequest.setBaseApiUrl("https://qa-api.company.com");
        agentRequest.setEndpointPath("/api/payments");
        agentRequest.setHttpMethod("POST");
        agentRequest.setFrameworkType("restassured-cucumber-serenity");
        agentRequest.setTestGenerationMode("deterministic");

        AutomationGenerationRequest request = new AutomationGenerationRequest();
        request.setAgentRequest(agentRequest);
        request.setTestCases(List.of(
                new TestCaseDto("TC_001", "Create payment with valid request", "Positive",
                        "Valid body", "201", "Payment is created", "High", "Ready", "JIRA+SWAGGER"),
                new TestCaseDto("TC_002", "Missing required accountId", "Negative",
                        "accountId missing", "400", "Validation error", "High", "Ready", "SWAGGER")
        ));
        return request;
    }
}
