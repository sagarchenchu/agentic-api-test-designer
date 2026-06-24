package com.agentic.api.service;

import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.TestCaseDto;
import com.agentic.api.model.TestMatrixResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContractExtractionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractTestMatrixService contractTestMatrixService;

    @Autowired
    private OpenApiParserService openApiParserService;

    @Test
    void extractContractEndpointReturnsContract() throws Exception {
        mockMvc.perform(post("/api/agent/extract-contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId", is("createPayment")))
                .andExpect(jsonPath("$.requestBody.requiredFields", hasSize(3)))
                .andExpect(jsonPath("$.requestBody.fields[?(@.name=='currency')].enumValues[0]", hasItem("USD")));
    }

    @Test
    void generateTestMatrixUsesSwaggerContract() throws Exception {
        mockMvc.perform(post("/api/agent/generate-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", not(empty())))
                .andExpect(jsonPath("$.testCases[?(@.scenarioName =~ /Missing required field: accountId/)]").exists())
                .andExpect(jsonPath("$.testCases[?(@.type=='Schema')]").exists())
                .andExpect(jsonPath("$.warnings", empty()));
    }

    @Test
    void generateTestMatrixFallsBackWithWarningWhenSwaggerInvalid() throws Exception {
        AgentRequest request = validPaymentRequest();
        request.setSwaggerJson("{ invalid json");

        mockMvc.perform(post("/api/agent/generate-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(5)))
                .andExpect(jsonPath("$.warnings", not(empty())));
    }

    @Test
    void extractContractReturns404ForMissingOperation() throws Exception {
        AgentRequest request = validPaymentRequest();
        request.setEndpointPath("/api/does-not-exist");

        mockMvc.perform(post("/api/agent/extract-contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("No operation found")));
    }

    @Test
    void contractTestMatrixGeneratesExpectedCaseTypes() throws Exception {
        AgentRequest request = validPaymentRequest();
        var contract = openApiParserService.extractContract(request);
        TestMatrixResponse response = contractTestMatrixService.generateFromContract(contract);

        List<String> types = response.getTestCases().stream().map(TestCaseDto::getType).toList();
        assertTrue(types.contains("Positive"));
        assertTrue(types.contains("Negative"));
        assertTrue(types.contains("Boundary"));
        assertTrue(types.contains("Security"));
        assertTrue(types.contains("Schema"));
    }

    private AgentRequest validPaymentRequest() throws Exception {
        String spec = new ClassPathResource("sample-openapi.json")
                .getContentAsString(StandardCharsets.UTF_8);
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setSwaggerJson(spec);
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setCredentialRef("qa_api_user");
        return request;
    }
}
