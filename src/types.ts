export type Environment = 'QA' | 'UAT' | 'DEV';

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type ExecutionMode =
  | 'generate-test-cases'
  | 'generate-automation'
  | 'generate-execute'
  | 'generate-execute-pr';

export type FrameworkType =
  | 'restassured-cucumber-serenity'
  | 'restassured-only'
  | 'playwright-api'
  | 'postman-newman'
  | 'custom';

export type TestType =
  | 'Positive'
  | 'Negative'
  | 'Boundary'
  | 'Security'
  | 'Schema'
  | 'Business';

export type StepStatus = 'pending' | 'running' | 'completed' | 'failed';

export type WorkspaceTab =
  | 'requirement-summary'
  | 'test-case-matrix'
  | 'generated-bdd'
  | 'generated-files'
  | 'execution-report';

export interface HeaderEntry {
  id: string;
  key: string;
  value: string;
}

export interface AgentFormValues {
  jiraStoryKey: string;
  jiraStoryText: string;
  swaggerUrl: string;
  swaggerJson: string;
  baseApiUrl: string;
  endpointPath: string;
  httpMethod: HttpMethod;
  headers: HeaderEntry[];
  credentialRef: string;
  projectPath: string;
  executionMode: ExecutionMode;
  frameworkType: FrameworkType;
}

export interface FormErrors {
  jiraStoryKey?: string;
  swagger?: string;
  baseApiUrl?: string;
  endpointPath?: string;
  httpMethod?: string;
  credentialRef?: string;
}

export interface RequirementSummary {
  jiraKey: string;
  endpoint: string;
  method: HttpMethod;
  requiredHeaders: string[];
  requestBodyFields: string[];
  expectedStatusCodes: string[];
  businessRules: string[];
  assumptions: string[];
}

export interface TestCase {
  id: string;
  scenarioName: string;
  type: TestType;
  inputVariation: string;
  expectedStatus: string;
  expectedValidation: string;
  priority: 'High' | 'Medium' | 'Low';
  automationStatus: string;
}

export interface GeneratedFile {
  path: string;
  content: string;
  language: string;
}

export interface FailedScenario {
  scenario: string;
  expected: string;
  actual: string;
  rootCause: string;
  endpoint: string;
  correlationId: string;
  responseBody: string;
}

export interface ExecutionResult {
  total: number;
  passed: number;
  failed: number;
  skipped: number;
  duration: string;
  reportPath: string;
  failedScenario: FailedScenario;
}

export interface TimelineStep {
  id: string;
  label: string;
  status: StepStatus;
}
