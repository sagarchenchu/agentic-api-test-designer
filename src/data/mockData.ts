import type {
  AgentFormValues,
  ExecutionMode,
  ExecutionResult,
  GeneratedFile,
  HttpMethod,
  RequirementSummary,
  TestCase,
  TimelineStep,
} from '../types';

export const defaultFormValues: AgentFormValues = {
  jiraStoryKey: '',
  jiraStoryText: '',
  swaggerUrl: '',
  swaggerJson: '',
  baseApiUrl: '',
  endpointPath: '',
  httpMethod: 'POST',
  headers: [
    { id: '1', key: 'Authorization', value: 'Bearer {{token}}' },
    { id: '2', key: 'Content-Type', value: 'application/json' },
    { id: '3', key: 'x-correlation-id', value: '{{$uuid}}' },
  ],
  credentialRef: '',
  projectPath: '',
  overwriteExisting: false,
  createBackup: true,
  testTag: '',
  mavenProfile: 'qa',
  timeoutSeconds: 300,
  executionMode: 'generate-execute',
  frameworkType: 'restassured-cucumber-serenity',
  testGenerationMode: 'deterministic',
};

export const baseTimelineSteps: TimelineStep[] = [
  { id: '1', label: 'Read Jira Story', status: 'pending' },
  { id: '2', label: 'Read Swagger Contract', status: 'pending' },
  { id: '3', label: 'Extract Requirement', status: 'pending' },
  { id: '4', label: 'Generate Test Matrix', status: 'pending' },
  { id: '5', label: 'Generate BDD', status: 'pending' },
  { id: '6', label: 'Generate Automation Files', status: 'pending' },
  { id: '7', label: 'Execute Tests', status: 'pending' },
  { id: '8', label: 'Analyze Results', status: 'pending' },
  { id: '9', label: 'Produce Report', status: 'pending' },
];

/** @deprecated Use getTimelineStepsForMode instead */
export const initialTimelineSteps = baseTimelineSteps;

const PR_STEP: TimelineStep = {
  id: '10',
  label: 'Create Pull Request',
  status: 'pending',
};

export function getTimelineStepsForMode(mode: ExecutionMode): TimelineStep[] {
  const steps = baseTimelineSteps.map((s) => ({ ...s }));
  if (mode === 'generate-execute-pr') {
    steps.push({ ...PR_STEP });
  }
  return steps;
}

export function shouldRunStep(label: string, mode: ExecutionMode): boolean {
  const testCasesOnly = new Set([
    'Read Jira Story',
    'Read Swagger Contract',
    'Extract Requirement',
    'Generate Test Matrix',
  ]);
  const automationOnly = new Set([
    'Read Jira Story',
    'Read Swagger Contract',
    'Extract Requirement',
    'Generate Test Matrix',
    'Generate BDD',
    'Generate Automation Files',
  ]);
  const executeSteps = new Set([
    'Read Jira Story',
    'Read Swagger Contract',
    'Extract Requirement',
    'Generate Test Matrix',
    'Generate BDD',
    'Generate Automation Files',
    'Execute Tests',
    'Analyze Results',
    'Produce Report',
  ]);

  switch (mode) {
    case 'generate-test-cases':
      return testCasesOnly.has(label);
    case 'generate-automation':
      return automationOnly.has(label);
    case 'generate-execute':
      return executeSteps.has(label);
    case 'generate-execute-pr':
      return executeSteps.has(label) || label === 'Create Pull Request';
    default:
      return false;
  }
}

export function shouldStopAfter(label: string, mode: ExecutionMode): boolean {
  switch (mode) {
    case 'generate-test-cases':
      return label === 'Generate Test Matrix';
    case 'generate-automation':
      return label === 'Generate Automation Files';
    case 'generate-execute':
      return label === 'Produce Report';
    case 'generate-execute-pr':
      return label === 'Create Pull Request';
    default:
      return false;
  }
}

export const mockTestCases: TestCase[] = [
  {
    id: 'TC_001',
    scenarioName: 'Create payment with valid request',
    type: 'Positive',
    inputVariation: 'Valid body',
    expectedStatus: '201',
    expectedValidation: 'Payment created',
    priority: 'High',
    automationStatus: 'Ready',
  },
  {
    id: 'TC_002',
    scenarioName: 'Missing required accountId',
    type: 'Negative',
    inputVariation: 'accountId missing',
    expectedStatus: '400',
    expectedValidation: 'Validation error',
    priority: 'High',
    automationStatus: 'Ready',
  },
  {
    id: 'TC_003',
    scenarioName: 'Invalid currency',
    type: 'Negative',
    inputVariation: 'currency = ABC',
    expectedStatus: '400',
    expectedValidation: 'Invalid currency error',
    priority: 'Medium',
    automationStatus: 'Ready',
  },
  {
    id: 'TC_004',
    scenarioName: 'Missing authorization header',
    type: 'Security',
    inputVariation: 'No auth token',
    expectedStatus: '401',
    expectedValidation: 'Unauthorized',
    priority: 'High',
    automationStatus: 'Ready',
  },
  {
    id: 'TC_005',
    scenarioName: 'Amount boundary zero',
    type: 'Boundary',
    inputVariation: 'amount = 0',
    expectedStatus: '400',
    expectedValidation: 'Amount validation',
    priority: 'Medium',
    automationStatus: 'Ready',
  },
];

function normalizeEndpoint(endpointPath: string): string {
  if (!endpointPath.trim()) return '/api/resource';
  return endpointPath.startsWith('/') ? endpointPath : `/${endpointPath}`;
}

function resourceFromEndpoint(endpoint: string): string {
  const segments = endpoint.split('/').filter(Boolean);
  return segments[segments.length - 1] ?? 'resource';
}

export function buildBddFeature(
  jiraKey: string,
  httpMethod: HttpMethod,
  endpointPath: string
): string {
  const key = jiraKey.trim() || 'STORY-000';
  const method = httpMethod.toUpperCase();
  const endpoint = normalizeEndpoint(endpointPath);
  const resource = resourceFromEndpoint(endpoint);
  const featureTitle = `${method} ${resource} API`;

  return `Feature: ${featureTitle}

@${key} @api @${resource}
Scenario: ${method} ${resource} with valid request
  Given user prepares "${resource}_valid" request
  When user sends ${method} request to "${endpoint}"
  Then response status code should be 201
  And response should match "${resource}_response_schema"
  And response should contain ${resource} id

@${key} @api @negative
Scenario: ${method} ${resource} without required field
  Given user prepares "${resource}_missing_required_field" request
  When user sends ${method} request to "${endpoint}"
  Then response status code should be 400
  And response should contain error field`;
}

export function buildGeneratedFiles(
  jiraKey: string,
  httpMethod: HttpMethod,
  endpointPath: string
): GeneratedFile[] {
  const endpoint = normalizeEndpoint(endpointPath);
  const resource = resourceFromEndpoint(endpoint);
  const method = httpMethod.toUpperCase();
  const bddContent = buildBddFeature(jiraKey, httpMethod, endpointPath);

  return [
    {
      path: `src/test/resources/features/${resource}/${resource}.feature`,
      language: 'gherkin',
      content: bddContent,
    },
    {
      path: `src/test/resources/templates/${resource}/${resource}_request.json`,
      language: 'json',
      content: `{
  "accountId": "{{accountId}}",
  "amount": 100.00,
  "currency": "USD",
  "description": "Test ${resource}"
}`,
    },
    {
      path: `src/test/resources/testdata/qa/${resource}/${resource}_data.json`,
      language: 'json',
      content: `{
  "${resource}_valid": {
    "accountId": "ACC-001",
    "amount": 250.00,
    "currency": "USD"
  },
  "${resource}_missing_required_field": {
    "amount": 250.00,
    "currency": "USD"
  }
}`,
    },
    {
      path: `src/test/resources/schemas/${resource}/${resource}_response_schema.json`,
      language: 'json',
      content: `{
  "type": "object",
  "required": ["${resource}Id", "status"],
  "properties": {
    "${resource}Id": { "type": "string" },
    "status": { "type": "string" }
  }
}`,
    },
    {
      path: `src/test/java/steps/${resource}/${capitalize(resource)}Steps.java`,
      language: 'java',
      content: `package steps.${resource};

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class ${capitalize(resource)}Steps {
    @Given("user prepares {string} request")
    public void prepareRequest(String template) {
        // Load request template
    }

    @When("user sends ${method} request to {string}")
    public void sendRequest(String endpoint) {
        // Execute API call
    }

    @Then("response status code should be {int}")
    public void verifyStatusCode(int status) {
        // Assert status
    }
}`,
    },
    {
      path: `src/test/java/api/${resource}/${capitalize(resource)}ApiClient.java`,
      language: 'java',
      content: `package api.${resource};

import io.restassured.RestAssured;

public class ${capitalize(resource)}ApiClient {
    public static void call${capitalize(resource)}(String baseUrl, Object body) {
        RestAssured.given()
            .baseUri(baseUrl)
            .body(body)
            .${method.toLowerCase()}("${endpoint}");
    }
}`,
    },
    {
      path: `src/test/java/validators/${resource}/${capitalize(resource)}Validator.java`,
      language: 'java',
      content: `package validators.${resource};

import io.restassured.response.Response;

public class ${capitalize(resource)}Validator {
    public static void assertCreated(Response response) {
        response.then().statusCode(201);
    }
}`,
    },
  ];
}

function capitalize(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

export function bddDownloadFilename(endpointPath: string): string {
  const resource = resourceFromEndpoint(normalizeEndpoint(endpointPath));
  return `${resource}.feature`;
}

export function buildRequirementSummary(
  jiraKey: string,
  baseUrl: string,
  endpoint: string,
  method: string,
  headers: { key: string; value: string }[]
): RequirementSummary {
  const normalizedEndpoint = normalizeEndpoint(endpoint);
  return {
    jiraKey: jiraKey.trim() || 'STORY-000',
    endpoint: `${baseUrl}${normalizedEndpoint}`,
    method: method as RequirementSummary['method'],
    requiredHeaders: headers.map((h) => `${h.key}: ${h.value}`),
    requestBodyFields: ['accountId', 'amount', 'currency', 'description'],
    expectedStatusCodes: ['201 Created', '400 Bad Request', '401 Unauthorized'],
    businessRules: [
      'Payment amount must be greater than zero',
      'Currency must be a valid ISO 4217 code',
      'Account must exist and be active',
    ],
    assumptions: [
      'API is available in the selected environment',
      'Credential reference resolves to a valid test user token',
      'Swagger contract matches deployed API version',
    ],
  };
}

export function buildExecutionResult(
  httpMethod: HttpMethod,
  endpointPath: string
): ExecutionResult {
  const endpoint = normalizeEndpoint(endpointPath);
  const resource = resourceFromEndpoint(endpoint);

  return {
    total: 5,
    passed: 4,
    failed: 1,
    skipped: 0,
    duration: '42 seconds',
    reportPath: 'target/site/serenity/index.html',
    failedScenario: {
      scenario: `${httpMethod} ${resource} with invalid input`,
      expected: '400',
      actual: '500',
      rootCause: 'API defect or missing validation handling',
      endpoint: `${httpMethod.toUpperCase()} ${endpoint}`,
      correlationId: 'abc-123',
      responseBody: '{"error":"Internal Server Error"}',
    },
  };
}
