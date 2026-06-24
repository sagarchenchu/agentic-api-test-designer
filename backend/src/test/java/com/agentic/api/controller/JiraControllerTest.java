package com.agentic.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JiraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void configStatusDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/agent/jira/config/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)))
                .andExpect(jsonPath("$.configured", is(false)))
                .andExpect(jsonPath("$.message", containsString("disabled")));
    }

    @Test
    void fetchStoryRejectsInvalidKey() throws Exception {
        mockMvc.perform(post("/api/agent/jira/fetch-story")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jiraStoryKey", "bad key"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void fetchStoryReturnsServiceUnavailableWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/agent/jira/fetch-story")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jiraStoryKey", "PAY-1234"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("disabled")));
    }
}
