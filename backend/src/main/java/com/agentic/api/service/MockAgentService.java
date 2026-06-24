package com.agentic.api.service;

import com.agentic.api.exception.ContractNotFoundException;
import com.agentic.api.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MockAgentService implements AgentService {

    private final Map<String, AgentRunResponse> runs = new ConcurrentHashMap<>();
    private final OpenApiParserService openApiParserService;
    private final ContractTestMatrixService contractTestMatrixService;
    private final AiTestMatrixService aiTestMatrixService;
    private final AutomationGenerationService automationGenerationService;
    private final FileWriteService fileWriteService;

    public MockAgentService(
            OpenApiParserService openApiParserService,
            ContractTestMatrixService contractTestMatrixService,
            AiTestMatrixService aiTestMatrixService,
            AutomationGenerationService automationGenerationService,
            FileWriteService fileWriteService
    ) {
        this.openApiParserService = openApiParserService;
        this.contractTestMatrixService = contractTestMatrixService;
        this.aiTestMatrixService = aiTestMatrixService;
        this.automationGenerationService = automationGenerationService;
        this.fileWriteService = fileWriteService;
    }

    @Override
    public TestMatrixResponse generateAiTestMatrix(AgentRequest request) {
        return aiTestMatrixService.generateAiTestMatrix(request);
    }

    @Override
    public TestMatrixResponse generateTestMatrix(AgentRequest request) {
        Optional<ApiContractDto> contract = openApiParserService.tryExtractContract(request);
        if (contract.isPresent()) {
            return contractTestMatrixService.generateFromContract(contract.get());
        }
        return new TestMatrixResponse(
                mockTestCases(),
                List.of("Swagger contract could not be parsed; using default mock test cases")
        );
    }

    private TestMatrixResponse resolveTestMatrix(AgentRequest request) {
        if ("ai-assisted".equalsIgnoreCase(request.getTestGenerationMode())) {
            return generateAiTestMatrix(request);
        }
        return generateTestMatrix(request);
    }

    @Override
    public ApiContractDto extractContract(AgentRequest request) {
        return openApiParserService.extractContract(request);
    }

    @Override
    public AutomationGenerationResponse generateAiBdd(AutomationGenerationRequest request) {
        return automationGenerationService.generateAiBdd(request);
    }

    @Override
    public AutomationGenerationResponse generateAiFiles(AutomationGenerationRequest request) {
        return automationGenerationService.generateAiFiles(request);
    }

    @Override
    public AutomationGenerationResponse generateAiAutomationPackage(AutomationGenerationRequest request) {
        return automationGenerationService.generateAiAutomationPackage(request);
    }

    @Override
    public FileWriteResponse previewFileWrite(FileWriteRequest request) {
        request.setWriteMode("preview");
        return fileWriteService.previewFileWrite(request);
    }

    @Override
    public FileWriteResponse writeGeneratedFiles(FileWriteRequest request) {
        request.setWriteMode("write");
        return fileWriteService.writeGeneratedFiles(request);
    }

    @Override
    public GeneratedBddDto generateBdd(AgentRequest request) {
        String content = buildBddFeature(
                request.getJiraStoryKey(),
                request.getHttpMethod(),
                request.getEndpointPath()
        );
        return new GeneratedBddDto(content, bddDownloadFilename(request.getEndpointPath()));
    }

    @Override
    public GeneratedFilesDto generateFiles(AgentRequest request) {
        GeneratedBddDto bdd = generateBdd(request);
        return new GeneratedFilesDto(
                buildGeneratedFiles(
                        request.getJiraStoryKey(),
                        request.getHttpMethod(),
                        request.getEndpointPath()
                ),
                bdd
        );
    }

    @Override
    public AgentRunResponse runAgent(AgentRequest request) {
        String mode = normalizeMode(request.getExecutionMode());
        String runId = UUID.randomUUID().toString();

        AgentRunResponse response = new AgentRunResponse();
        response.setRunId(runId);
        response.setStatus("completed");
        response.setRequirementSummary(buildRequirementSummary(request));
        response.setTimelineSteps(buildTimelineSteps(mode));

        if (shouldIncludeTestMatrix(mode)) {
            TestMatrixResponse matrix = resolveTestMatrix(request);
            response.setTestCases(matrix.getTestCases());
            response.setTestMatrixWarnings(matrix.getWarnings());
            response.setTestMatrixAssumptions(matrix.getAssumptions());
        }

        if (shouldIncludeBdd(mode)) {
            response.setGeneratedBdd(generateBdd(request));
        }

        if (shouldIncludeFiles(mode)) {
            response.setGeneratedFiles(buildGeneratedFiles(
                    request.getJiraStoryKey(),
                    request.getHttpMethod(),
                    request.getEndpointPath()
            ));
        }

        if (shouldIncludeExecutionReport(mode)) {
            response.setExecutionReport(buildExecutionReport(
                    request.getHttpMethod(),
                    request.getEndpointPath()
            ));
        }

        runs.put(runId, response);
        return response;
    }

    @Override
    public AgentRunResponse getRun(String runId) {
        AgentRunResponse response = runs.get(runId);
        if (response == null) {
            throw new NoSuchElementException("Run not found: " + runId);
        }
        return response;
    }

    private String normalizeMode(String executionMode) {
        if (executionMode == null || executionMode.isBlank()) {
            return "generate-execute";
        }
        return executionMode;
    }

    private boolean shouldIncludeTestMatrix(String mode) {
        return Set.of(
                "generate-test-cases",
                "generate-automation",
                "generate-execute",
                "generate-execute-pr"
        ).contains(mode);
    }

    private boolean shouldIncludeBdd(String mode) {
        return Set.of(
                "generate-automation",
                "generate-execute",
                "generate-execute-pr"
        ).contains(mode);
    }

    private boolean shouldIncludeFiles(String mode) {
        return shouldIncludeBdd(mode);
    }

    private boolean shouldIncludeExecutionReport(String mode) {
        return Set.of("generate-execute", "generate-execute-pr").contains(mode);
    }

    private List<TimelineStepDto> buildTimelineSteps(String mode) {
        List<TimelineStepDto> steps = new ArrayList<>(List.of(
                step("1", "Read Jira Story"),
                step("2", "Read Swagger Contract"),
                step("3", "Extract Requirement"),
                step("4", "Generate Test Matrix"),
                step("5", "Generate BDD"),
                step("6", "Generate Automation Files"),
                step("7", "Execute Tests"),
                step("8", "Analyze Results"),
                step("9", "Produce Report")
        ));

        if ("generate-execute-pr".equals(mode)) {
            steps.add(step("10", "Create Pull Request"));
        }

        Set<String> runnable = runnableSteps(mode);
        for (TimelineStepDto step : steps) {
            step.setStatus(runnable.contains(step.getLabel()) ? "completed" : "pending");
        }
        return steps;
    }

    private Set<String> runnableSteps(String mode) {
        return switch (mode) {
            case "generate-test-cases" -> Set.of(
                    "Read Jira Story", "Read Swagger Contract", "Extract Requirement",
                    "Generate Test Matrix"
            );
            case "generate-automation" -> Set.of(
                    "Read Jira Story", "Read Swagger Contract", "Extract Requirement",
                    "Generate Test Matrix", "Generate BDD", "Generate Automation Files"
            );
            case "generate-execute" -> Set.of(
                    "Read Jira Story", "Read Swagger Contract", "Extract Requirement",
                    "Generate Test Matrix", "Generate BDD", "Generate Automation Files",
                    "Execute Tests", "Analyze Results", "Produce Report"
            );
            case "generate-execute-pr" -> Set.of(
                    "Read Jira Story", "Read Swagger Contract", "Extract Requirement",
                    "Generate Test Matrix", "Generate BDD", "Generate Automation Files",
                    "Execute Tests", "Analyze Results", "Produce Report", "Create Pull Request"
            );
            default -> Set.of();
        };
    }

    private TimelineStepDto step(String id, String label) {
        return new TimelineStepDto(id, label, "pending");
    }

    private RequirementSummaryDto buildRequirementSummary(AgentRequest request) {
        String endpoint = normalizeEndpoint(request.getEndpointPath());
        RequirementSummaryDto summary = new RequirementSummaryDto();
        summary.setJiraKey(request.getJiraStoryKey().trim());
        summary.setEndpoint(request.getBaseApiUrl() + endpoint);
        summary.setMethod(request.getHttpMethod().toUpperCase());
        summary.setRequiredHeaders(request.getHeaders().stream()
                .map(h -> h.getKey() + ": " + h.getValue())
                .collect(Collectors.toList()));
        summary.setRequestBodyFields(List.of("accountId", "amount", "currency", "description"));
        summary.setExpectedStatusCodes(List.of("201 Created", "400 Bad Request", "401 Unauthorized"));
        summary.setBusinessRules(List.of(
                "Payment amount must be greater than zero",
                "Currency must be a valid ISO 4217 code",
                "Account must exist and be active"
        ));
        summary.setAssumptions(List.of(
                "API is available in the selected environment",
                "Credential reference resolves to a valid test user token",
                "Swagger contract matches deployed API version"
        ));
        return summary;
    }

    private List<TestCaseDto> mockTestCases() {
        return List.of(
                new TestCaseDto("TC_001", "Create payment with valid request", "Positive",
                        "Valid body", "201", "Payment created", "High", "Ready"),
                new TestCaseDto("TC_002", "Missing required accountId", "Negative",
                        "accountId missing", "400", "Validation error", "High", "Ready"),
                new TestCaseDto("TC_003", "Invalid currency", "Negative",
                        "currency = ABC", "400", "Invalid currency error", "Medium", "Ready"),
                new TestCaseDto("TC_004", "Missing authorization header", "Security",
                        "No auth token", "401", "Unauthorized", "High", "Ready"),
                new TestCaseDto("TC_005", "Amount boundary zero", "Boundary",
                        "amount = 0", "400", "Amount validation", "Medium", "Ready")
        );
    }

    private String buildBddFeature(String jiraKey, String httpMethod, String endpointPath) {
        String key = jiraKey == null ? "" : jiraKey.trim();
        if (key.isEmpty()) {
            key = "STORY-000";
        }
        String method = httpMethod.toUpperCase();
        String endpoint = normalizeEndpoint(endpointPath);
        String resource = resourceFromEndpoint(endpoint);
        String featureTitle = method + " " + resource + " API";

        return """
                Feature: %s

                @%s @api @%s
                Scenario: %s %s with valid request
                  Given user prepares "%s_valid" request
                  When user sends %s request to "%s"
                  Then response status code should be 201
                  And response should match "%s_response_schema"
                  And response should contain %s id

                @%s @api @negative
                Scenario: %s %s without required field
                  Given user prepares "%s_missing_required_field" request
                  When user sends %s request to "%s"
                  Then response status code should be 400
                  And response should contain error field
                """.formatted(
                featureTitle, key, resource,
                method, resource, resource, method, endpoint, resource, resource,
                key, method, resource, resource, method, endpoint
        );
    }

    private List<GeneratedFileDto> buildGeneratedFiles(String jiraKey, String httpMethod, String endpointPath) {
        String endpoint = normalizeEndpoint(endpointPath);
        String resource = resourceFromEndpoint(endpoint);
        String method = httpMethod.toUpperCase();
        String bddContent = buildBddFeature(jiraKey, httpMethod, endpointPath);
        String capResource = capitalize(resource);

        return List.of(
                new GeneratedFileDto(
                        "src/test/resources/features/" + resource + "/" + resource + ".feature",
                        bddContent, "gherkin"),
                new GeneratedFileDto(
                        "src/test/resources/templates/" + resource + "/" + resource + "_request.json",
                        """
                                {
                                  "accountId": "{{accountId}}",
                                  "amount": 100.00,
                                  "currency": "USD",
                                  "description": "Test %s"
                                }""".formatted(resource), "json"),
                new GeneratedFileDto(
                        "src/test/resources/testdata/qa/" + resource + "/" + resource + "_data.json",
                        """
                                {
                                  "%s_valid": {
                                    "accountId": "ACC-001",
                                    "amount": 250.00,
                                    "currency": "USD"
                                  },
                                  "%s_missing_required_field": {
                                    "amount": 250.00,
                                    "currency": "USD"
                                  }
                                }""".formatted(resource, resource), "json"),
                new GeneratedFileDto(
                        "src/test/resources/schemas/" + resource + "/" + resource + "_response_schema.json",
                        """
                                {
                                  "type": "object",
                                  "required": ["%sId", "status"],
                                  "properties": {
                                    "%sId": { "type": "string" },
                                    "status": { "type": "string" }
                                  }
                                }""".formatted(resource, resource), "json"),
                new GeneratedFileDto(
                        "src/test/java/steps/" + resource + "/" + capResource + "Steps.java",
                        """
                                package steps.%s;

                                import io.cucumber.java.en.Given;
                                import io.cucumber.java.en.When;
                                import io.cucumber.java.en.Then;

                                public class %sSteps {
                                    @Given("user prepares {string} request")
                                    public void prepareRequest(String template) {
                                        // Load request template
                                    }

                                    @When("user sends %s request to {string}")
                                    public void sendRequest(String endpoint) {
                                        // Execute API call
                                    }

                                    @Then("response status code should be {int}")
                                    public void verifyStatusCode(int status) {
                                        // Assert status
                                    }
                                }""".formatted(resource, capResource, method), "java"),
                new GeneratedFileDto(
                        "src/test/java/api/" + resource + "/" + capResource + "ApiClient.java",
                        """
                                package api.%s;

                                import io.restassured.RestAssured;

                                public class %sApiClient {
                                    public static void call%s(String baseUrl, Object body) {
                                        RestAssured.given()
                                            .baseUri(baseUrl)
                                            .body(body)
                                            .%s("%s");
                                    }
                                }""".formatted(resource, capResource, capResource, method.toLowerCase(), endpoint), "java"),
                new GeneratedFileDto(
                        "src/test/java/validators/" + resource + "/" + capResource + "Validator.java",
                        """
                                package validators.%s;

                                import io.restassured.response.Response;

                                public class %sValidator {
                                    public static void assertCreated(Response response) {
                                        response.then().statusCode(201);
                                    }
                                }""".formatted(resource, capResource), "java")
        );
    }

    private ExecutionReportDto buildExecutionReport(String httpMethod, String endpointPath) {
        String endpoint = normalizeEndpoint(endpointPath);
        String resource = resourceFromEndpoint(endpoint);

        FailedScenarioDto failed = new FailedScenarioDto();
        failed.setScenario(httpMethod.toUpperCase() + " " + resource + " with invalid input");
        failed.setExpected("400");
        failed.setActual("500");
        failed.setRootCause("API defect or missing validation handling");
        failed.setEndpoint(httpMethod.toUpperCase() + " " + endpoint);
        failed.setCorrelationId("abc-123");
        failed.setResponseBody("{\"error\":\"Internal Server Error\"}");

        ExecutionReportDto report = new ExecutionReportDto();
        report.setTotal(5);
        report.setPassed(4);
        report.setFailed(1);
        report.setSkipped(0);
        report.setDuration("42 seconds");
        report.setReportPath("target/site/serenity/index.html");
        report.setFailedScenario(failed);
        return report;
    }

    private String normalizeEndpoint(String endpointPath) {
        if (endpointPath == null || endpointPath.isBlank()) {
            return "/api/resource";
        }
        return endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
    }

    private String resourceFromEndpoint(String endpoint) {
        String[] segments = endpoint.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (!segments[i].isBlank()) {
                return segments[i];
            }
        }
        return "resource";
    }

    private String bddDownloadFilename(String endpointPath) {
        return resourceFromEndpoint(normalizeEndpoint(endpointPath)) + ".feature";
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
