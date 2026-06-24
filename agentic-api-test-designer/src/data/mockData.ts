import type {
  AgentFormValues,
  ExecutionResult,
  GeneratedFile,
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
  executionMode: 'generate-execute',
  frameworkType: 'restassured-cucumber-serenity',
};

export const initialTimelineSteps: TimelineStep[] = [
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

export const mockBddFeature = `Feature: Create Payment API

@PAY-1234 @api @payment
Scenario: Create payment with valid request
  Given user prepares "create_payment_valid" request
  When user sends POST request to "/api/payments"
  Then response status code should be 201
  And response should match "create_payment_response_schema"
  And response should contain payment id

@PAY-1234 @api @negative
Scenario: Create payment without accountId
  Given user prepares "create_payment_missing_account_id" request
  When user sends POST request to "/api/payments"
  Then response status code should be 400
  And response should contain error field "accountId"`;

export const mockGeneratedFiles: GeneratedFile[] = [
  {
    path: 'src/test/resources/features/payment/create_payment.feature',
    language: 'gherkin',
    content: mockBddFeature,
  },
  {
    path: 'src/test/resources/templates/payment/create_payment_request.json',
    language: 'json',
    content: `{
  "accountId": "{{accountId}}",
  "amount": 100.00,
  "currency": "USD",
  "description": "Test payment"
}`,
  },
  {
    path: 'src/test/resources/testdata/qa/payment/create_payment_data.json',
    language: 'json',
    content: `{
  "create_payment_valid": {
    "accountId": "ACC-001",
    "amount": 250.00,
    "currency": "USD"
  },
  "create_payment_missing_account_id": {
    "amount": 250.00,
    "currency": "USD"
  }
}`,
  },
  {
    path: 'src/test/resources/schemas/payment/create_payment_response_schema.json',
    language: 'json',
    content: `{
  "type": "object",
  "required": ["paymentId", "status"],
  "properties": {
    "paymentId": { "type": "string" },
    "status": { "type": "string" }
  }
}`,
  },
  {
    path: 'src/test/java/steps/payment/PaymentSteps.java',
    language: 'java',
    content: `package steps.payment;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

public class PaymentSteps {
    @Given("user prepares {string} request")
    public void prepareRequest(String template) {
        // Load request template
    }

    @When("user sends POST request to {string}")
    public void sendPostRequest(String endpoint) {
        // Execute API call
    }

    @Then("response status code should be {int}")
    public void verifyStatusCode(int status) {
        // Assert status
    }
}`,
  },
  {
    path: 'src/test/java/api/payment/PaymentApiClient.java',
    language: 'java',
    content: `package api.payment;

import io.restassured.RestAssured;

public class PaymentApiClient {
    public static void createPayment(String baseUrl, Object body) {
        RestAssured.given()
            .baseUri(baseUrl)
            .body(body)
            .post("/api/payments");
    }
}`,
  },
  {
    path: 'src/test/java/validators/payment/PaymentValidator.java',
    language: 'java',
    content: `package validators.payment;

import io.restassured.response.Response;

public class PaymentValidator {
    public static void assertPaymentCreated(Response response) {
        response.then().statusCode(201);
    }
}`,
  },
];

export function buildRequirementSummary(
  jiraKey: string,
  baseUrl: string,
  endpoint: string,
  method: string,
  headers: { key: string; value: string }[]
): RequirementSummary {
  return {
    jiraKey: jiraKey || 'PAY-1234',
    endpoint: `${baseUrl}${endpoint}`,
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

export const mockExecutionResult: ExecutionResult = {
  total: 5,
  passed: 4,
  failed: 1,
  skipped: 0,
  duration: '42 seconds',
  reportPath: 'target/site/serenity/index.html',
  failedScenario: {
    scenario: 'Create payment with invalid currency',
    expected: '400',
    actual: '500',
    rootCause: 'API defect or missing validation handling',
    endpoint: 'POST /api/payments',
    correlationId: 'abc-123',
    responseBody: '{"error":"Internal Server Error"}',
  },
};
