import type {
  AgentFormValues,
  ExecutionResult,
  GeneratedFile,
  RequirementSummary,
  TestCase,
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
}

export interface GeneratedBddResponse {
  content: string;
  downloadFilename: string;
}

export interface GeneratedFilesResponse {
  files: GeneratedFile[];
  generatedBdd: GeneratedBddResponse;
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
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

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
  };
}

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
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

export async function generateTestMatrix(
  request: AgentRequest
): Promise<TestCase[]> {
  return apiFetch<TestCase[]>('/api/agent/generate-test-matrix', {
    method: 'POST',
    body: JSON.stringify(request),
  });
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
