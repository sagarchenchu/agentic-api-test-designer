import type {
  AgentFormValues,
  ApiContract,
  ExecutionResult,
  FileWriteResponse,
  GeneratedFile,
  GitPrResponse,
  JiraConfigStatus,
  JiraOperationResponse,
  JiraStoryDetails,
  RunHistoryDetail,
  RunHistorySummary,
  RequirementSummary,
  TestCase,
  TestExecutionResponse,
  TimelineStep,
} from '../types';

export interface AgentRequest {
  jiraStoryKey: string;
  jiraStoryText: string;
  swaggerUrl: string;
  swaggerJson: string;
  baseApiUrl: string;
  endpointPath: string;
  httpMethod: string;
  headers: { key: string; value: string }[];
  credentialRef: string;
  projectPath: string;
  executionMode: string;
  frameworkType: string;
  testGenerationMode?: string;
}

export interface GeneratedBddResponse {
  content: string;
  downloadFilename: string;
}

export interface GeneratedFilesResponse {
  files: GeneratedFile[];
  generatedBdd: GeneratedBddResponse;
}

export interface TestMatrixResponse {
  testCases: TestCase[];
  warnings: string[];
  assumptions?: string[];
}

export interface AutomationGenerationRequest {
  agentRequest: AgentRequest;
  apiContract?: ApiContract | null;
  testCases?: TestCase[];
}

export interface AutomationGenerationResponse {
  generatedBdd: GeneratedBddResponse;
  generatedFiles: GeneratedFile[];
  warnings: string[];
  assumptions: string[];
  source: string;
  fallbackUsed: boolean;
}

export interface FileWriteRequest {
  projectPath: string;
  files: GeneratedFile[];
  /** Informational only — each endpoint enforces preview vs write server-side. */
  writeMode?: 'preview' | 'write';
  overwriteExisting?: boolean;
  createBackup?: boolean;
}

export interface TestExecutionRequest {
  projectPath: string;
  commandType?: string;
  mavenCommand?: string;
  testTag?: string;
  profile?: string;
  timeoutSeconds?: number;
  environment?: string;
  dryRun?: boolean;
}

export interface GitPrRequest {
  projectPath: string;
  jiraStoryKey: string;
  baseBranch?: string;
  newBranchName?: string;
  commitMessage?: string;
  prTitle?: string;
  prBody?: string;
  remoteName?: string;
  filesToCommit: string[];
  dryRun?: boolean;
}

export interface JiraPostSummaryRequest {
  jiraStoryKey: string;
  testCaseCount: number;
  bddGenerated: boolean;
  filesWritten: number;
  executionStatus?: string;
  passed: number;
  failed: number;
  prUrl?: string;
  serenityReportPath?: string;
}

export interface JiraLinkPrRequest {
  jiraStoryKey: string;
  prUrl: string;
}

export interface AgentRunResponse {
  runId: string;
  status: string;
  requirementSummary: RequirementSummary;
  testCases: TestCase[];
  generatedBdd: GeneratedBddResponse | null;
  generatedFiles: GeneratedFile[];
  executionReport: ExecutionResult | null;
  timelineSteps: TimelineStep[];
  testMatrixWarnings?: string[];
  testMatrixAssumptions?: string[];
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const API_TOKEN = import.meta.env.VITE_AGENTIC_API_TOKEN ?? '';

function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = {};
  if (API_TOKEN) {
    headers['X-Agentic-Token'] = API_TOKEN;
  }
  return headers;
}

export class AgentApiError extends Error {
  status?: number;
  details?: unknown;

  constructor(message: string, status?: number, details?: unknown) {
    super(message);
    this.name = 'AgentApiError';
    this.status = status;
    this.details = details;
  }
}

export function formValuesToRequest(values: AgentFormValues): AgentRequest {
  return {
    jiraStoryKey: values.jiraStoryKey,
    jiraStoryText: values.jiraStoryText,
    swaggerUrl: values.swaggerUrl,
    swaggerJson: values.swaggerJson,
    baseApiUrl: values.baseApiUrl,
    endpointPath: values.endpointPath,
    httpMethod: values.httpMethod,
    headers: values.headers.map(({ key, value }) => ({ key, value })),
    credentialRef: values.credentialRef,
    projectPath: values.projectPath,
    executionMode: values.executionMode,
    frameworkType: values.frameworkType,
    testGenerationMode: values.testGenerationMode,
  };
}

export function buildAutomationRequest(
  values: AgentFormValues,
  apiContract?: ApiContract | null,
  testCases?: TestCase[]
): AutomationGenerationRequest {
  return {
    agentRequest: formValuesToRequest(values),
    apiContract: apiContract ?? undefined,
    testCases: testCases?.length ? testCases : undefined,
  };
}

export function buildFileWriteRequest(
  values: AgentFormValues,
  files: GeneratedFile[]
): FileWriteRequest {
  return {
    projectPath: values.projectPath,
    files,
    overwriteExisting: values.overwriteExisting,
    createBackup: values.createBackup,
  };
}

export function buildTestExecutionRequest(
  values: AgentFormValues,
  environment = 'QA'
): TestExecutionRequest {
  const jiraKey = values.jiraStoryKey.trim();
  const testTag =
    values.testTag.trim() || (jiraKey ? `@${jiraKey}` : '');

  return {
    projectPath: values.projectPath,
    commandType: 'MAVEN',
    mavenCommand: 'mvn clean verify',
    testTag,
    profile: values.mavenProfile,
    timeoutSeconds: values.timeoutSeconds,
    environment,
    dryRun: false,
  };
}

export function deriveFilesToCommit(
  generatedFiles: GeneratedFile[],
  fileWritePreview: FileWriteResponse | null
): string[] {
  const writtenPaths = fileWritePreview?.results
    .filter((result) => result.status === 'WRITTEN')
    .map((result) => result.relativePath) ?? [];

  if (writtenPaths.length > 0) {
    return [...new Set(writtenPaths)];
  }

  return generatedFiles.map((file) => file.path);
}

export function buildGitPrRequest(
  values: AgentFormValues,
  filesToCommit: string[]
): GitPrRequest {
  const jiraKey = values.jiraStoryKey.trim() || 'STORY-000';

  return {
    projectPath: values.projectPath,
    jiraStoryKey: jiraKey,
    baseBranch: values.baseBranch.trim() || 'main',
    newBranchName:
      values.newBranchName.trim() || `feature/${jiraKey}-api-tests`,
    commitMessage:
      values.commitMessage.trim() || `Add API automation tests for ${jiraKey}`,
    prTitle: values.prTitle.trim() || `${jiraKey} Add API automation tests`,
    prBody:
      values.prBody.trim() || 'Generated API tests from Jira + Swagger.',
    remoteName: values.remoteName.trim() || 'origin',
    filesToCommit,
    dryRun: false,
  };
}

export function buildJiraStoryText(story: JiraStoryDetails): string {
  const lines: string[] = [
    `Summary: ${story.summary}`,
    `Status: ${story.status}`,
    `Type: ${story.issueType}`,
    `Priority: ${story.priority}`,
  ];
  if (story.labels.length > 0) {
    lines.push(`Labels: ${story.labels.join(', ')}`);
  }
  if (story.components.length > 0) {
    lines.push(`Components: ${story.components.join(', ')}`);
  }
  if (story.epicKey) {
    lines.push(`Epic: ${story.epicKey}`);
  }
  lines.push('', 'Description:', story.description || '(none)');
  if (story.acceptanceCriteria.length > 0) {
    lines.push('', 'Acceptance Criteria:');
    for (const item of story.acceptanceCriteria) {
      lines.push(`- ${item}`);
    }
  }
  return lines.join('\n');
}

export function buildJiraPostSummaryRequest(
  jiraStoryKey: string,
  testCases: TestCase[],
  bddContent: string,
  fileWritePreview: FileWriteResponse | null,
  testExecutionResult: TestExecutionResponse | null,
  gitPrResult: GitPrResponse | null
): JiraPostSummaryRequest {
  return {
    jiraStoryKey,
    testCaseCount: testCases.length,
    bddGenerated: bddContent.trim().length > 0,
    filesWritten: fileWritePreview?.summary.written ?? 0,
    executionStatus: testExecutionResult?.status,
    passed: testExecutionResult?.summary.passed ?? 0,
    failed: testExecutionResult?.summary.failed ?? 0,
    prUrl: gitPrResult?.prUrl ?? undefined,
    serenityReportPath: testExecutionResult?.reportPaths.serenity ?? undefined,
  };
}

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...options?.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let details: unknown;
    try {
      details = await response.json();
    } catch {
      details = await response.text();
    }
    throw new AgentApiError(
      `API request failed: ${response.status} ${response.statusText}`,
      response.status,
      details
    );
  }

  return response.json() as Promise<T>;
}

export async function extractContract(
  request: AgentRequest
): Promise<ApiContract> {
  return apiFetch<ApiContract>('/api/agent/extract-contract', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateTestMatrix(
  request: AgentRequest
): Promise<TestMatrixResponse> {
  return apiFetch<TestMatrixResponse>('/api/agent/generate-test-matrix', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateAiTestMatrix(
  request: AgentRequest
): Promise<TestMatrixResponse> {
  return apiFetch<TestMatrixResponse>('/api/agent/generate-ai-test-matrix', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateAiBdd(
  request: AutomationGenerationRequest
): Promise<AutomationGenerationResponse> {
  return apiFetch<AutomationGenerationResponse>('/api/agent/generate-ai-bdd', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateAiFiles(
  request: AutomationGenerationRequest
): Promise<AutomationGenerationResponse> {
  return apiFetch<AutomationGenerationResponse>('/api/agent/generate-ai-files', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateAiAutomationPackage(
  request: AutomationGenerationRequest
): Promise<AutomationGenerationResponse> {
  return apiFetch<AutomationGenerationResponse>(
    '/api/agent/generate-ai-automation-package',
    {
      method: 'POST',
      body: JSON.stringify(request),
    }
  );
}

export async function previewFileWrite(
  request: FileWriteRequest
): Promise<FileWriteResponse> {
  return apiFetch<FileWriteResponse>('/api/agent/preview-file-write', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function writeGeneratedFiles(
  request: FileWriteRequest
): Promise<FileWriteResponse> {
  return apiFetch<FileWriteResponse>('/api/agent/write-generated-files', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function previewTestExecution(
  request: TestExecutionRequest
): Promise<TestExecutionResponse> {
  return apiFetch<TestExecutionResponse>('/api/agent/preview-test-execution', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function runTestExecution(
  request: TestExecutionRequest
): Promise<TestExecutionResponse> {
  return apiFetch<TestExecutionResponse>('/api/agent/run-test-execution', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getTestExecution(
  executionId: string
): Promise<TestExecutionResponse> {
  return apiFetch<TestExecutionResponse>(`/api/agent/test-executions/${executionId}`);
}

export async function previewGitPr(
  request: GitPrRequest
): Promise<GitPrResponse> {
  return apiFetch<GitPrResponse>('/api/agent/preview-git-pr', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function createGitPr(
  request: GitPrRequest
): Promise<GitPrResponse> {
  return apiFetch<GitPrResponse>('/api/agent/create-git-pr', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getGitPr(operationId: string): Promise<GitPrResponse> {
  return apiFetch<GitPrResponse>(`/api/agent/git-pr/${operationId}`);
}

export async function getJiraConfigStatus(): Promise<JiraConfigStatus> {
  return apiFetch<JiraConfigStatus>('/api/agent/jira/config/status');
}

export async function fetchJiraStory(jiraStoryKey: string): Promise<JiraStoryDetails> {
  return apiFetch<JiraStoryDetails>('/api/agent/jira/fetch-story', {
    method: 'POST',
    body: JSON.stringify({ jiraStoryKey }),
  });
}

export async function postJiraSummary(
  request: JiraPostSummaryRequest
): Promise<JiraOperationResponse> {
  return apiFetch<JiraOperationResponse>('/api/agent/jira/post-summary', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function linkPrToJira(
  request: JiraLinkPrRequest
): Promise<JiraOperationResponse> {
  return apiFetch<JiraOperationResponse>('/api/agent/jira/link-pr', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function listRunHistory(): Promise<RunHistorySummary[]> {
  return apiFetch<RunHistorySummary[]>('/api/agent/history/runs');
}

export async function getRunHistory(runId: string): Promise<RunHistoryDetail> {
  return apiFetch<RunHistoryDetail>(`/api/agent/history/runs/${runId}`);
}

export async function deleteRunHistory(runId: string): Promise<{ status: string; runId: string }> {
  return apiFetch<{ status: string; runId: string }>(`/api/agent/history/runs/${runId}`, {
    method: 'DELETE',
  });
}

export async function getRunArtifacts(runId: string): Promise<RunHistoryDetail['artifacts']> {
  return apiFetch<RunHistoryDetail['artifacts']>(`/api/agent/history/runs/${runId}/artifacts`);
}

export async function generateBdd(
  request: AgentRequest
): Promise<GeneratedBddResponse> {
  return apiFetch<GeneratedBddResponse>('/api/agent/generate-bdd', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function generateFiles(
  request: AgentRequest
): Promise<GeneratedFilesResponse> {
  return apiFetch<GeneratedFilesResponse>('/api/agent/generate-files', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function runAgent(
  request: AgentRequest
): Promise<AgentRunResponse> {
  return apiFetch<AgentRunResponse>('/api/agent/run', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getAgentRun(runId: string): Promise<AgentRunResponse> {
  return apiFetch<AgentRunResponse>(`/api/agent/runs/${runId}`);
}

export async function isBackendAvailable(): Promise<boolean> {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 2000);
    const response = await fetch(`${API_BASE_URL}/api/agent/health`, {
      method: 'GET',
      signal: controller.signal,
    });
    clearTimeout(timeout);
    return response.ok;
  } catch {
    return false;
  }
}
