package com.agentic.api.controller;

import com.agentic.api.security.ApiTokenAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.enabled=true",
        "security.api-token=test-token-123"
})
class ApiTokenAuthEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/agent/history/runs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void acceptsValidToken() throws Exception {
        mockMvc.perform(get("/api/agent/history/runs")
                        .header(ApiTokenAuthFilter.TOKEN_HEADER, "test-token-123"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointBypassesAuth() throws Exception {
        mockMvc.perform(get("/api/agent/health"))
                .andExpect(status().isOk());
    }
}
