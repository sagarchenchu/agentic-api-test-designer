package com.agentic.api.controller;

import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.HeaderEntryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateTestMatrixReturnsMockCases() throws Exception {
        mockMvc.perform(post("/api/agent/generate-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(5)))
                .andExpect(jsonPath("$.testCases[0].id", is("TC_001")))
                .andExpect(jsonPath("$.warnings", not(empty())));
    }

    @Test
    void generateBddUsesDynamicInputs() throws Exception {
        AgentRequest request = validRequest();
        request.setJiraStoryKey("ORD-99");
        request.setHttpMethod("PUT");
        request.setEndpointPath("/api/orders");

        mockMvc.perform(post("/api/agent/generate-bdd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("@ORD-99")))
                .andExpect(jsonPath("$.content", containsString("PUT request to \"/api/orders\"")))
                .andExpect(jsonPath("$.downloadFilename", is("orders.feature")));
    }

    @Test
    void generateFilesReturnsFileTreeAndBdd() throws Exception {
        mockMvc.perform(post("/api/agent/generate-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files", hasSize(7)))
                .andExpect(jsonPath("$.files[0].path", containsString("payments")))
                .andExpect(jsonPath("$.generatedBdd.content", containsString("@PAY-1234")))
                .andExpect(jsonPath("$.generatedBdd.downloadFilename", is("payments.feature")));
    }

    @Test
    void runAgentReturnsFullResponse() throws Exception {
        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId", not(emptyString())))
                .andExpect(jsonPath("$.status", is("completed")))
                .andExpect(jsonPath("$.requirementSummary.jiraKey", is("PAY-1234")))
                .andExpect(jsonPath("$.testCases", hasSize(5)))
                .andExpect(jsonPath("$.generatedBdd.content", containsString("@PAY-1234")))
                .andExpect(jsonPath("$.generatedFiles", hasSize(7)))
                .andExpect(jsonPath("$.executionReport.passed", is(4)))
                .andExpect(jsonPath("$.timelineSteps", not(empty())));
    }

    @Test
    void getRunReturnsStoredRun() throws Exception {
        String body = mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String runId = objectMapper.readTree(body).get("runId").asText();

        mockMvc.perform(get("/api/agent/runs/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId", is(runId)));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        AgentRequest request = new AgentRequest();
        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.jiraStoryKey", notNullValue()));
    }

    @Test
    void rejectsPasswordLikeCredentialRef() throws Exception {
        AgentRequest request = validRequest();
        request.setCredentialRef("my_password_ref");

        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.credentialRef", notNullValue()));
    }

    @Test
    void rejectsInvalidHttpMethod() throws Exception {
        AgentRequest request = validRequest();
        request.setHttpMethod("TRACE");

        mockMvc.perform(post("/api/agent/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.httpMethod", notNullValue()));
    }

    private AgentRequest validRequest() {
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setJiraStoryText("As a user...");
        request.setSwaggerUrl("https://qa-api.company.com/swagger.json");
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setHeaders(List.of(
                new HeaderEntryDto("Authorization", "Bearer {{token}}"),
                new HeaderEntryDto("Content-Type", "application/json")
        ));
        request.setCredentialRef("qa_api_user");
        request.setProjectPath("C:\\repos\\api-automation-framework");
        request.setExecutionMode("generate-execute");
        request.setFrameworkType("restassured-cucumber-serenity");
        return request;
    }
}
