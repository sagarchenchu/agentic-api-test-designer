package com.agentic.api.service;

import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.ApiContractDto;
import com.agentic.api.model.ApiFieldDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiParserServiceTest {

    private OpenApiParserService parserService;
    private String sampleSpec;

    @BeforeEach
    void setUp() throws Exception {
        parserService = new OpenApiParserService(new ObjectMapper());
        sampleSpec = new ClassPathResource("sample-openapi.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void extractsPostPaymentsContract() {
        AgentRequest request = paymentRequest("/api/payments", "POST");

        ApiContractDto contract = parserService.extractContract(request);

        assertEquals("/api/payments", contract.getEndpointPath());
        assertEquals("POST", contract.getHttpMethod());
        assertEquals("createPayment", contract.getOperationId());
        assertEquals("Create payment", contract.getSummary());
        assertEquals("Creates a payment transaction", contract.getDescription());
        assertEquals(List.of("Payments"), contract.getTags());
        assertEquals(2, contract.getRequiredHeaders().size());
        assertNotNull(contract.getRequestBody());
        assertEquals(List.of("accountId", "amount", "currency"),
                contract.getRequestBody().getRequiredFields());
    }

    @Test
    void resolvesRequestSchemaRefAndExtractsFields() {
        ApiContractDto contract = parserService.extractContract(
                paymentRequest("/api/payments", "POST"));

        ApiFieldDto amount = contract.getRequestBody().getFields().stream()
                .filter(f -> "amount".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("number", amount.getType());
        assertEquals(0.01, amount.getMinimum());
        assertEquals(100.00, amount.getExample());

        ApiFieldDto currency = contract.getRequestBody().getFields().stream()
                .filter(f -> "currency".equals(f.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("USD", "EUR", "INR"), currency.getEnumValues());
    }

    @Test
    void extractsResponses() {
        ApiContractDto contract = parserService.extractContract(
                paymentRequest("/api/payments", "POST"));

        assertTrue(contract.getResponses().stream()
                .anyMatch(r -> "201".equals(r.getStatusCode())));
        var created = contract.getResponses().stream()
                .filter(r -> "201".equals(r.getStatusCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("paymentId", "status"), created.getFields());
    }

    @Test
    void templatePathMatchesConcreteInstance() {
        AgentRequest request = new AgentRequest();
        request.setSwaggerJson(sampleSpec);
        request.setJiraStoryKey("ORD-1");
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath("/api/orders/123");
        request.setHttpMethod("GET");

        ApiContractDto contract = parserService.extractContract(request);

        assertEquals("/api/orders/{orderId}", contract.getEndpointPath());
        assertEquals("getOrder", contract.getOperationId());
        assertEquals(1, contract.getPathParams().size());
        assertEquals("orderId", contract.getPathParams().get(0).getName());
    }

    @Test
    void rejectsMissingEndpoint() {
        AgentRequest request = paymentRequest("/api/unknown", "POST");
        assertThrows(Exception.class, () -> parserService.extractContract(request));
    }

    @Test
    void pathsMatchSupportsTemplateSegments() {
        assertTrue(parserService.pathsMatch("/api/orders/{orderId}", "/api/orders/123", true));
        assertFalse(parserService.pathsMatch("/api/orders/{orderId}", "/api/orders/123/extra", true));
        assertTrue(parserService.pathsMatch("/api/payments", "/api/payments", false));
    }

    private AgentRequest paymentRequest(String endpoint, String method) {
        AgentRequest request = new AgentRequest();
        request.setSwaggerJson(sampleSpec);
        request.setJiraStoryKey("PAY-1234");
        request.setBaseApiUrl("https://qa-api.company.com");
        request.setEndpointPath(endpoint);
        request.setHttpMethod(method);
        return request;
    }
}
