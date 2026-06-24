package com.agentic.reference.controller;

import com.agentic.reference.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService.reset();
    }

    @Test
    void createPaymentReturns201ForValidRequest() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-1234"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", containsString("PAY-")))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    @Test
    void createPaymentReturns400ForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Missing or invalid")));
    }

    @Test
    void createPaymentReturns401ForMissingAuthorization() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-401"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentReturns401ForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer wrong-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-401b"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentReturns403ForForbiddenClient() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "blocked-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-403"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPaymentReturns409ForDuplicateInvoiceId() throws Exception {
        String body = objectMapper.writeValueAsString(validPaymentBody("INV-DUP"));
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-1")
                        .header("X-Client-Id", "web-portal")
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-2")
                        .header("X-Client-Id", "web-portal")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createPaymentReturns422ForUnsupportedCurrency() throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>(validPaymentBody("INV-422"));
        body.put("currency", "JPY");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Unsupported currency")));
    }

    @Test
    void createPaymentReturns500WhenTriggerClientIdUsed() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "trigger-500")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-500"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPaymentReturns404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/PAY-MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentReturns200ForExistingPayment() throws Exception {
        String createResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-GET"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = objectMapper.readTree(createResponse).get("paymentId").asText();

        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)));
    }

    @Test
    void updatePaymentStatusReturns404WhenNotFound() throws Exception {
        mockMvc.perform(put("/api/payments/PAY-MISSING/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CAPTURED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePaymentStatusReturns200ForExistingPayment() throws Exception {
        String createResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .header("X-Correlation-Id", "corr-123")
                        .header("X-Client-Id", "web-portal")
                        .content(objectMapper.writeValueAsString(validPaymentBody("INV-PUT"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = objectMapper.readTree(createResponse).get("paymentId").asText();

        mockMvc.perform(put("/api/payments/" + paymentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CAPTURED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CAPTURED")));
    }

    @Test
    void staticOpenApiYamlIsServed() throws Exception {
        mockMvc.perform(get("/openapi/reference-api.yaml"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("operationId: createPayment")));
    }

    private Map<String, Object> validPaymentBody(String invoiceId) {
        return Map.of(
                "accountId", "ACC-1001",
                "amount", 125.50,
                "currency", "USD",
                "paymentMethod", Map.of("type", "CARD", "token", "tok_visa_123"),
                "billingAddress", Map.of(
                        "line1", "123 Main St",
                        "city", "Phoenix",
                        "state", "AZ",
                        "zip", "85022"
                ),
                "metadata", Map.of("invoiceId", invoiceId)
        );
    }
}
