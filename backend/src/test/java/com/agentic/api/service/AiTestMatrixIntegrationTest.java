package com.agentic.api.service;

import com.agentic.api.model.AgentRequest;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiTestMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAiClientService openAiClientService;

    @Test
    void endpointFallsBackWhenOpenAiDisabled() throws Exception {
        when(openAiClientService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/agent/generate-ai-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", not(empty())))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("OpenAI not configured"))));
    }

    @Test
    void endpointReturnsMockedAiMatrix() throws Exception {
        String aiResponse = new ClassPathResource("sample-ai-test-matrix.json")
                .getContentAsString(StandardCharsets.UTF_8);
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(aiResponse);

        mockMvc.perform(post("/api/agent/generate-ai-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.testCases[0].source", is("JIRA+SWAGGER")))
                .andExpect(jsonPath("$.assumptions", hasSize(1)));
    }

    @Test
    void runAgentUsesAiMatrixWhenModeAiAssisted() throws Exception {
        String aiResponse = new ClassPathResource("sample-ai-test-matrix.json")
                .getContentAsString(StandardCharsets.UTF_8);
        when(openAiClientService.isEnabled()).thenReturn(true);
        when(openAiClientService.completeJson(anyString(), anyString())).thenReturn(aiResponse);

        AgentRequest request = validRequest();
        request.setTestGenerationMode("ai-assisted");

        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.testCases[0].source", is("JIRA+SWAGGER")))
                .andExpect(jsonPath("$.testMatrixAssumptions", hasSize(1)));
    }

    private AgentRequest validRequest() throws Exception {
        String spec = new ClassPathResource("sample-openapi.json")
                .getContentAsString(StandardCharsets.UTF_8);
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setJiraStoryText("Amount should not exceed daily limit of 5000");
        request.setSwaggerJson(spec);
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setCredentialRef("qa_api_user");
        request.setFrameworkType("restassured-cucumber-serenity");
        return request;
    }
}
