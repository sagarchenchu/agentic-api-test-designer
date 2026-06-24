package com.agentic.api.service;

import com.agentic.api.model.FileWriteRequest;
import com.agentic.api.model.GeneratedFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileWriteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private Path projectRoot;

    @BeforeEach
    void setUp() throws Exception {
        projectRoot = tempDir.resolve("automation-project");
        Files.createDirectories(projectRoot.resolve("src/test"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
    }

    @Test
    void previewFileWriteEndpointReturnsCreateAction() throws Exception {
        mockMvc.perform(post("/api/agent/preview-file-write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeRequest(
                                "src/test/resources/features/payment/create_payment.feature",
                                "Feature: Payment"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.create", is(1)))
                .andExpect(jsonPath("$.results[0].action", is("CREATE")))
                .andExpect(jsonPath("$.results[0].status", is("READY")))
                .andExpect(jsonPath("$.results[0].diff", containsString("+++")));
    }

    @Test
    void writeGeneratedFilesEndpointWritesFile() throws Exception {
        mockMvc.perform(post("/api/agent/write-generated-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeRequest(
                                "src/test/resources/templates/payment/create_payment_request.json",
                                "{\"accountId\":\"ACC-001\"}"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.written", is(1)))
                .andExpect(jsonPath("$.results[0].status", is("WRITTEN")));

        Path written = projectRoot.resolve("src/test/resources/templates/payment/create_payment_request.json");
        assertTrue(Files.exists(written));
    }

    private FileWriteRequest writeRequest(String path, String content) {
        FileWriteRequest request = new FileWriteRequest();
        request.setProjectPath(projectRoot.toString());
        request.setFiles(List.of(new GeneratedFileDto(path, content, "json")));
        request.setWriteMode("preview");
        return request;
    }
}
