package com.agentic.api.service;

import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.ApiContractDto;
import com.agentic.api.model.AutomationGenerationRequest;
import com.agentic.api.model.FileWriteRequest;
import com.agentic.api.model.GeneratedFileDto;
import com.agentic.api.model.TestCaseDto;
import com.agentic.api.model.TestExecutionRequest;
import com.agentic.api.model.TestMatrixResponse;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReferenceHarnessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpenApiParserService openApiParserService;

    @Autowired
    private ContractTestMatrixService contractTestMatrixService;

    @Autowired
    private AutomationGenerationService automationGenerationService;

    @TempDir
    Path tempDir;

    private Path projectRoot;
    private String referenceOpenApiSpec;

    @BeforeEach
    void setUp() throws Exception {
        Path specPath = repoRoot().resolve("samples/reference-api/openapi/reference-api.yaml");
        assumeTrue(Files.exists(specPath), "Reference OpenAPI spec must exist at " + specPath);
        referenceOpenApiSpec = Files.readString(specPath, StandardCharsets.UTF_8);

        projectRoot = tempDir.resolve("reference-automation-project");
        Files.createDirectories(projectRoot.resolve("src/test/resources/features"));
        Files.createDirectories(projectRoot.resolve("src/test/java"));
        Files.copy(
                repoRoot().resolve("samples/reference-automation-project/pom.xml"),
                projectRoot.resolve("pom.xml")
        );
    }

    @Test
    void referenceOpenApiExtractsCreatePaymentContract() throws Exception {
        ApiContractDto contract = openApiParserService.extractContract(referenceAgentRequest());

        org.junit.jupiter.api.Assertions.assertEquals("createPayment", contract.getOperationId());
        org.junit.jupiter.api.Assertions.assertTrue(
                contract.getRequestBody().getRequiredFields().size() >= 3
        );
    }

    @Test
    void referenceOpenApiGeneratesDeterministicTestMatrix() throws Exception {
        ApiContractDto contract = openApiParserService.extractContract(referenceAgentRequest());
        TestMatrixResponse matrix = contractTestMatrixService.generateFromContract(contract);

        List<String> types = matrix.getTestCases().stream().map(TestCaseDto::getType).distinct().toList();
        assertTrue(types.contains("Positive"));
        assertTrue(types.contains("Negative"));
        assertTrue(matrix.getTestCases().size() >= 5);
    }

    @Test
    void referenceHarnessGenerateAutomationPackageSmoke() throws Exception {
        AutomationGenerationRequest request = referenceAutomationRequest();
        var response = automationGenerationService.generateAiAutomationPackage(request);

        org.junit.jupiter.api.Assertions.assertFalse(response.getGeneratedFiles().isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                response.getGeneratedBdd().getContent().contains("Feature:")
        );
        assertTrue(response.getGeneratedFiles().stream()
                .anyMatch(file -> file.getPath().contains("features/payment")));
    }

    @Test
    void referenceHarnessPreviewAndWriteGeneratedFiles() throws Exception {
        List<GeneratedFileDto> files = automationGenerationService.generateAiAutomationPackage(referenceAutomationRequest())
                .getGeneratedFiles();

        FileWriteRequest previewRequest = new FileWriteRequest();
        previewRequest.setProjectPath(projectRoot.toString());
        previewRequest.setFiles(files);

        mockMvc.perform(post("/api/agent/preview-file-write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.create", greaterThan(0)))
                .andExpect(jsonPath("$.results[0].status", is("READY")));

        FileWriteRequest writeRequest = new FileWriteRequest();
        writeRequest.setProjectPath(projectRoot.toString());
        writeRequest.setFiles(files);

        mockMvc.perform(post("/api/agent/write-generated-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(writeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.written", greaterThan(0)));

        GeneratedFileDto featureFile = files.stream()
                .filter(file -> file.getPath().contains("features/") && file.getPath().endsWith(".feature"))
                .findFirst()
                .orElseThrow();
        assertTrue(Files.exists(projectRoot.resolve(featureFile.getPath())));
    }

    @Test
    void referenceHarnessPreviewTestExecution() throws Exception {
        TestExecutionRequest request = new TestExecutionRequest();
        request.setProjectPath(projectRoot.toString());
        request.setCommandType("MAVEN");
        request.setMavenCommand("mvn test");
        request.setTestTag("@PAY-REF-001");
        request.setProfile("qa");
        request.setEnvironment("QA");
        request.setDryRun(true);

        mockMvc.perform(post("/api/agent/preview-test-execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.command", containsString("mvn")));
    }

    @Test
    void referenceOpenApiEndpointExtractContract() throws Exception {
        mockMvc.perform(post("/api/agent/extract-contract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(referenceAgentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId", is("createPayment")))
                .andExpect(jsonPath("$.requestBody.requiredFields", not(empty())));
    }

    @Test
    void referenceOpenApiEndpointGenerateTestMatrix() throws Exception {
        mockMvc.perform(post("/api/agent/generate-test-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(referenceAgentRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCases", hasSize(greaterThan(4))))
                .andExpect(jsonPath("$.testCases[?(@.type=='Schema')]").exists());
    }

    private AgentRequest referenceAgentRequest() {
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-REF-001");
        request.setSwaggerJson(referenceOpenApiSpec);
        request.setBaseApiUrl("http://localhost:9090");
        request.setEndpointPath("/api/payments");
        request.setHttpMethod("POST");
        request.setCredentialRef("reference_api_user");
        return request;
    }

    private AutomationGenerationRequest referenceAutomationRequest() throws Exception {
        AgentRequest agentRequest = referenceAgentRequest();
        ApiContractDto contract = openApiParserService.extractContract(agentRequest);
        TestMatrixResponse matrix = contractTestMatrixService.generateFromContract(contract);

        AutomationGenerationRequest request = new AutomationGenerationRequest();
        request.setAgentRequest(agentRequest);
        request.setApiContract(contract);
        request.setTestCases(matrix.getTestCases());
        return request;
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if ("backend".equals(cwd.getFileName().toString())) {
            return cwd.getParent();
        }
        return cwd;
    }
}
