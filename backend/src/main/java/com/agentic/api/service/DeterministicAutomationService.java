package com.agentic.api.service;

import com.agentic.api.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DeterministicAutomationService {

    public GeneratedBddDto generateBdd(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases
    ) {
        String content = buildBddFeature(request, contract, testCases);
        return new GeneratedBddDto(content, bddDownloadFilename(contract, request.getEndpointPath()));
    }

    public List<GeneratedFileDto> generateFiles(
            AgentRequest request,
            ApiContractDto contract,
            List<TestCaseDto> testCases,
            GeneratedBddDto bdd
    ) {
        String endpoint = normalizeEndpoint(contract.getEndpointPath() != null
                ? contract.getEndpointPath()
                : request.getEndpointPath());
        String resource = singularResource(resourceFromEndpoint(endpoint));
        String operation = operationName(request.getHttpMethod(), resource);
        String method = request.getHttpMethod().toUpperCase();
        String capResource = capitalize(resource);
        String capOperation = capitalize(operation);

        List<GeneratedFileDto> files = new ArrayList<>();
        files.add(new GeneratedFileDto(
                "src/test/resources/features/" + resource + "/" + operation + ".feature",
                bdd.getContent(),
                "gherkin"
        ));
        files.add(new GeneratedFileDto(
                "src/test/resources/templates/" + resource + "/" + operation + "_request.json",
                buildRequestTemplate(resource, operation),
                "json"
        ));
        files.add(new GeneratedFileDto(
                "src/test/resources/testdata/qa/" + resource + "/" + operation + "_data.json",
                buildTestData(resource, operation, testCases),
                "json"
        ));
        files.add(new GeneratedFileDto(
                "src/test/resources/schemas/" + resource + "/" + operation + "_response_schema.json",
                buildResponseSchema(resource),
                "json"
        ));
        files.add(new GeneratedFileDto(
                "src/test/java/steps/" + resource + "/" + capOperation + "Steps.java",
                buildStepsClass(resource, operation, capOperation, method),
                "java"
        ));
        files.add(new GeneratedFileDto(
                "src/test/java/api/" + resource + "/" + capOperation + "ApiClient.java",
                buildApiClientClass(resource, operation, capOperation, method, endpoint),
                "java"
        ));
        files.add(new GeneratedFileDto(
                "src/test/java/validators/" + resource + "/" + capOperation + "Validator.java",
                buildValidatorClass(resource, capOperation),
                "java"
        ));
        return files;
    }

    String buildBddFeature(AgentRequest request, ApiContractDto contract, List<TestCaseDto> testCases) {
        String jiraKey = normalizeJiraKey(request.getJiraStoryKey());
        String endpoint = normalizeEndpoint(contract.getEndpointPath() != null
                ? contract.getEndpointPath()
                : request.getEndpointPath());
        String method = request.getHttpMethod().toUpperCase();
        String resource = singularResource(resourceFromEndpoint(endpoint));
        String featureTitle = capitalize(method) + " " + resource + " API";

        StringBuilder sb = new StringBuilder();
        sb.append("@").append(jiraKey).append(" @api @").append(resource).append("\n");
        sb.append("Feature: ").append(featureTitle).append("\n\n");
        sb.append("  Background:\n");
        sb.append("    Given the API base URL is configured\n");
        sb.append("    And the \"Authorization\" header is available\n\n");

        List<TestCaseDto> cases = testCases.isEmpty() ? defaultTestCases(method, resource) : testCases;
        for (TestCaseDto testCase : cases) {
            String typeTag = typeTag(testCase.getType());
            sb.append("  @").append(testCase.getId()).append(" @").append(typeTag).append("\n");
            sb.append("  Scenario: ").append(testCase.getScenarioName()).append("\n");
            if ("Positive".equalsIgnoreCase(testCase.getType())) {
                sb.append("    Given I prepare a valid ").append(method)
                        .append(" request for \"").append(endpoint).append("\"\n");
                appendBodySteps(sb, contract, true);
            } else {
                sb.append("    Given I prepare a ").append(method)
                        .append(" request for \"").append(endpoint).append("\"\n");
                appendBodySteps(sb, contract, false);
            }
            sb.append("    When I send the request\n");
            sb.append("    Then the response status code should be ")
                    .append(testCase.getExpectedStatus()).append("\n");
            if (testCase.getExpectedValidation() != null && !testCase.getExpectedValidation().isBlank()) {
                sb.append("    And ").append(testCase.getExpectedValidation()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim() + "\n";
    }

    private void appendBodySteps(StringBuilder sb, ApiContractDto contract, boolean valid) {
        ApiRequestBodyDto body = contract.getRequestBody();
        if (body == null || body.getFields() == null || body.getFields().isEmpty()) {
            return;
        }
        if (valid) {
            for (ApiFieldDto field : body.getFields()) {
                if (body.getRequiredFields() != null && body.getRequiredFields().contains(field.getName())) {
                    sb.append("    And I set request body field \"")
                            .append(field.getName()).append("\" to ")
                            .append(sampleValue(field)).append("\n");
                }
            }
        } else if (body.getRequiredFields() != null && !body.getRequiredFields().isEmpty()) {
            String field = body.getRequiredFields().get(0);
            sb.append("    And I remove request body field \"").append(field).append("\"\n");
        }
    }

    private String sampleValue(ApiFieldDto field) {
        if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
            return "\"" + field.getEnumValues().get(0) + "\"";
        }
        return switch (field.getType() != null ? field.getType().toLowerCase(Locale.ROOT) : "string") {
            case "integer", "int", "long" -> "100";
            case "number", "float", "double" -> "100.00";
            case "boolean" -> "true";
            default -> "\"sample-" + field.getName() + "\"";
        };
    }

    private List<TestCaseDto> defaultTestCases(String method, String resource) {
        return List.of(
                new TestCaseDto("TC_001", capitalize(method) + " " + resource + " with valid request",
                        "Positive", "Valid body", "201", "the response should match the expected schema",
                        "High", "Ready"),
                new TestCaseDto("TC_002", "Missing required field",
                        "Negative", "Required field missing", "400",
                        "the response should contain validation error", "High", "Ready")
        );
    }

    private String buildRequestTemplate(String resource, String operation) {
        return """
                {
                  "accountId": "{{accountId}}",
                  "amount": 100.00,
                  "currency": "USD",
                  "description": "Test %s"
                }""".formatted(resource);
    }

    private String buildTestData(String resource, String operation, List<TestCaseDto> testCases) {
        StringBuilder sb = new StringBuilder("{\n");
        int index = 0;
        for (TestCaseDto testCase : testCases) {
            if (index > 0) {
                sb.append(",\n");
            }
            String key = testCase.getId().toLowerCase(Locale.ROOT).replace("-", "_");
            sb.append("  \"").append(key).append("\": {\n");
            sb.append("    \"scenario\": \"").append(escapeJson(testCase.getScenarioName())).append("\",\n");
            sb.append("    \"inputVariation\": \"").append(escapeJson(testCase.getInputVariation())).append("\"\n");
            sb.append("  }");
            index++;
        }
        if (index == 0) {
            sb.append("  \"").append(operation).append("_valid\": {\n");
            sb.append("    \"accountId\": \"ACC-001\",\n");
            sb.append("    \"amount\": 250.00,\n");
            sb.append("    \"currency\": \"USD\"\n");
            sb.append("  }");
        }
        sb.append("\n}");
        return sb.toString();
    }

    private String buildResponseSchema(String resource) {
        return """
                {
                  "type": "object",
                  "required": ["%sId", "status"],
                  "properties": {
                    "%sId": { "type": "string" },
                    "status": { "type": "string" }
                  }
                }""".formatted(resource, resource);
    }

    private String buildStepsClass(String resource, String operation, String capOperation, String method) {
        return """
                package steps.%s;

                import io.cucumber.java.en.Given;
                import io.cucumber.java.en.When;
                import io.cucumber.java.en.Then;

                public class %sSteps {
                    @Given("the API base URL is configured")
                    public void configureBaseUrl() {
                        // Load base URL from configuration
                    }

                    @Given("the {string} header is available")
                    public void headerAvailable(String headerName) {
                        // Resolve header from credential reference
                    }

                    @Given("I prepare a valid %s request for {string}")
                    public void prepareValidRequest(String endpoint) {
                        // Load request template for %s
                    }

                    @Given("I prepare a %s request for {string}")
                    public void prepareRequest(String endpoint) {
                        // Load request template for %s
                    }

                    @Given("I set request body field {string} to {double}")
                    public void setBodyFieldDouble(String field, double value) {
                        // Set request body field
                    }

                    @Given("I set request body field {string} to {string}")
                    public void setBodyFieldString(String field, String value) {
                        // Set request body field
                    }

                    @Given("I remove request body field {string}")
                    public void removeBodyField(String field) {
                        // Remove request body field
                    }

                    @When("I send the request")
                    public void sendRequest() {
                        // Execute API call
                    }

                    @Then("the response status code should be {int}")
                    public void verifyStatusCode(int status) {
                        // Assert status
                    }
                }""".formatted(
                resource, capOperation, method, operation, method, operation
        );
    }

    private String buildApiClientClass(
            String resource, String operation, String capOperation, String method, String endpoint
    ) {
        return """
                package api.%s;

                import io.restassured.RestAssured;
                import io.restassured.response.Response;

                public class %sApiClient {
                    public static Response call%s(String baseUrl, Object body) {
                        return RestAssured.given()
                            .baseUri(baseUrl)
                            .body(body)
                            .%s("%s");
                    }
                }""".formatted(resource, capOperation, capOperation, method.toLowerCase(Locale.ROOT), endpoint);
    }

    private String buildValidatorClass(String resource, String capOperation) {
        return """
                package validators.%s;

                import io.restassured.response.Response;

                public class %sValidator {
                    public static void assertStatus(Response response, int expectedStatus) {
                        response.then().statusCode(expectedStatus);
                    }

                    public static void assertSchema(Response response, String schemaPath) {
                        // Validate response against JSON schema
                    }
                }""".formatted(resource, capOperation);
    }

    private String typeTag(String type) {
        if (type == null || type.isBlank()) {
            return "business";
        }
        return type.toLowerCase(Locale.ROOT);
    }

    private String normalizeJiraKey(String jiraKey) {
        if (jiraKey == null || jiraKey.isBlank()) {
            return "STORY-000";
        }
        return jiraKey.trim();
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
            if (!segments[i].isBlank() && !segments[i].startsWith("{")) {
                return segments[i];
            }
        }
        return "resource";
    }

    private String singularResource(String resource) {
        if (resource.endsWith("ies")) {
            return resource.substring(0, resource.length() - 3) + "y";
        }
        if (resource.endsWith("s") && resource.length() > 1) {
            return resource.substring(0, resource.length() - 1);
        }
        return resource;
    }

    private String operationName(String httpMethod, String resource) {
        String verb = switch (httpMethod.toUpperCase(Locale.ROOT)) {
            case "POST" -> "create";
            case "GET" -> "get";
            case "PUT" -> "update";
            case "PATCH" -> "patch";
            case "DELETE" -> "delete";
            default -> httpMethod.toLowerCase(Locale.ROOT);
        };
        return verb + "_" + resource;
    }

    private String bddDownloadFilename(ApiContractDto contract, String fallbackEndpoint) {
        String endpoint = contract.getEndpointPath() != null ? contract.getEndpointPath() : fallbackEndpoint;
        String resource = singularResource(resourceFromEndpoint(normalizeEndpoint(endpoint)));
        return resource + ".feature";
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
