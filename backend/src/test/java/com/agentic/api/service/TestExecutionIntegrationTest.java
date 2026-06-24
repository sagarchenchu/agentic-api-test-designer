package com.agentic.api.service;

import com.agentic.api.model.TestExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessRunnerService processRunnerService;

    @TempDir
    Path tempDir;

    private Path projectRoot;

    @BeforeEach
    void setUp() throws Exception {
        projectRoot = tempDir.resolve("automation-project");
        Files.createDirectories(projectRoot);
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
    }

    @Test
    void previewTestExecutionEndpointReturnsReady() throws Exception {
        mockMvc.perform(post("/api/agent/preview-test-execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.command", containsString("mvn clean verify")))
                .andExpect(jsonPath("$.command", containsString("@PAY-1234")));
    }

    @Test
    void runTestExecutionEndpointReturnsStoredExecution() throws Exception {
        ProcessRunnerService.ProcessRunResult result = new ProcessRunnerService.ProcessRunResult();
        result.setExitCode(0);
        result.setOutputLines(List.of("BUILD SUCCESS"));
        when(processRunnerService.run(anyList(), any(Path.class), anyInt())).thenReturn(result);

        String body = mockMvc.perform(post("/api/agent/run-test-execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PASSED")))
                .andExpect(jsonPath("$.executionId", not(emptyString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String executionId = objectMapper.readTree(body).get("executionId").asText();

        mockMvc.perform(get("/api/agent/test-executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(executionId)))
                .andExpect(jsonPath("$.status", is("PASSED")));
    }

    private TestExecutionRequest validRequest() {
        TestExecutionRequest request = new TestExecutionRequest();
        request.setProjectPath(projectRoot.toString());
        request.setMavenCommand("mvn clean verify");
        request.setTestTag("@PAY-1234");
        request.setProfile("qa");
        request.setEnvironment("QA");
        request.setTimeoutSeconds(300);
        return request;
    }
}
