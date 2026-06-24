import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  AgentFormValues,
  ApiContract,
  Environment,
  ExecutionResult,
  FormErrors,
  FileWriteResponse,
  GeneratedFile,
  RequirementSummary,
  TestCase,
  TestExecutionResponse,
  GitPrResponse,
  JiraConfigStatus,
  JiraStoryDetails,
  RunHistoryDetail,
  RunHistorySummary,
  RunArtifact,
  TimelineStep,
  WorkspaceTab,
} from './types';
import {
  defaultFormValues,
  getTimelineStepsForMode,
  shouldRunStep,
  shouldStopAfter,
  mockTestCases,
  buildBddFeature,
  buildGeneratedFiles,
  buildRequirementSummary,
  buildExecutionResult,
  bddDownloadFilename,
} from './data/mockData';
import {
  formValuesToRequest,
  buildAutomationRequest,
  buildFileWriteRequest,
  buildTestExecutionRequest,
  buildGitPrRequest,
  deriveFilesToCommit,
  extractContract,
  generateBdd,
  generateAiBdd,
  generateAiFiles,
  generateAiAutomationPackage,
  previewFileWrite,
  writeGeneratedFiles,
  previewTestExecution,
  runTestExecution,
  previewGitPr,
  createGitPr,
  getJiraConfigStatus,
  fetchJiraStory,
  postJiraSummary,
  linkPrToJira,
  listRunHistory,
  getRunHistory,
  deleteRunHistory,
  getRunArtifacts,
  buildJiraStoryText,
  buildJiraPostSummaryRequest,
  generateTestMatrix,
  generateAiTestMatrix,
  runAgent,
  isBackendAvailable,
  AgentApiError,
} from './api/agentApi';
import type { AgentRunResponse, AutomationGenerationResponse } from './api/agentApi';
import Header from './components/Header';
import AgentInputPanel from './components/AgentInputPanel';
import WorkspaceTabs from './components/WorkspaceTabs';
import AgentTimeline from './components/AgentTimeline';
import FooterStatusBar from './components/FooterStatusBar';

const PASSWORD_PATTERN =
  /password|passwd|pwd|secret|api[_-]?key/i;

function validateForm(values: AgentFormValues): FormErrors {
  const errors: FormErrors = {};

  if (!values.jiraStoryKey.trim()) {
    errors.jiraStoryKey = 'Jira Story Key is required.';
  }

  if (!values.swaggerUrl.trim() && !values.swaggerJson.trim()) {
    errors.swagger = 'Provide either a Swagger/OpenAPI URL or JSON.';
  }

  if (!values.baseApiUrl.trim()) {
    errors.baseApiUrl = 'Base API URL is required.';
  }

  if (!values.endpointPath.trim()) {
    errors.endpointPath = 'Endpoint Path is required.';
  }

  if (!values.httpMethod) {
    errors.httpMethod = 'HTTP Method is required.';
  }

  if (values.credentialRef.trim() && PASSWORD_PATTERN.test(values.credentialRef)) {
    errors.credentialRef =
      'Credential reference should not contain password-like values. Use a reference name only.';
  }

  return errors;
}

function applyJiraGitDefaults(values: AgentFormValues): AgentFormValues {
  const jiraKey = values.jiraStoryKey.trim();
  if (!jiraKey) {
    return values;
  }
  return {
    ...values,
    newBranchName: values.newBranchName || `feature/${jiraKey}-api-tests`,
    commitMessage: values.commitMessage || `Add API automation tests for ${jiraKey}`,
    prTitle: values.prTitle || `${jiraKey} Add API automation tests`,
  };
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function applyRunResponse(
  response: AgentRunResponse,
  mode: AgentFormValues['executionMode'],
  setters: {
    setRequirementSummary: (v: RequirementSummary | null) => void;
    setTestCases: (v: TestCase[]) => void;
    setBddContent: (v: string) => void;
    setGeneratedFiles: (v: GeneratedFile[]) => void;
    setSelectedFile: (v: GeneratedFile | null) => void;
    setExecutionResult: (v: ExecutionResult | null) => void;
    setTimelineSteps: (v: TimelineStep[]) => void;
    setActiveTab: (v: WorkspaceTab) => void;
    setMatrixWarnings: (v: string[]) => void;
    setMatrixAssumptions: (v: string[]) => void;
  }
) {
  setters.setRequirementSummary(response.requirementSummary);
  setters.setTimelineSteps(response.timelineSteps);
  setters.setMatrixWarnings(response.testMatrixWarnings ?? []);
  setters.setMatrixAssumptions(response.testMatrixAssumptions ?? []);

  if (response.testCases?.length) {
    setters.setTestCases(response.testCases);
  }

  if (response.generatedBdd) {
    setters.setBddContent(response.generatedBdd.content);
  }

  if (response.generatedFiles?.length) {
    setters.setGeneratedFiles(response.generatedFiles);
    setters.setSelectedFile(response.generatedFiles[0]);
  }

  if (response.executionReport) {
    setters.setExecutionResult(response.executionReport);
    setters.setActiveTab('execution-report');
  } else if (response.generatedFiles?.length) {
    setters.setActiveTab('generated-files');
  } else if (response.generatedBdd) {
    setters.setActiveTab('generated-bdd');
  } else if (response.testCases?.length) {
    setters.setActiveTab('test-case-matrix');
  }

  if (mode === 'generate-execute-pr') {
    // PR step status already reflected in timelineSteps from backend
  }
}

export default function App() {
  const [environment, setEnvironment] = useState<Environment>('QA');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [formValues, setFormValues] = useState<AgentFormValues>(defaultFormValues);
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [activeTab, setActiveTab] = useState<WorkspaceTab>('requirement-summary');
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [matrixWarnings, setMatrixWarnings] = useState<string[]>([]);
  const [matrixAssumptions, setMatrixAssumptions] = useState<string[]>([]);
  const [automationWarnings, setAutomationWarnings] = useState<string[]>([]);
  const [automationAssumptions, setAutomationAssumptions] = useState<string[]>([]);
  const [bddContent, setBddContent] = useState('');
  const [generatedFiles, setGeneratedFiles] = useState<GeneratedFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<GeneratedFile | null>(null);
  const [requirementSummary, setRequirementSummary] =
    useState<RequirementSummary | null>(null);
  const [apiContract, setApiContract] = useState<ApiContract | null>(null);
  const [apiContractError, setApiContractError] = useState<string | null>(null);
  const [executionResult, setExecutionResult] =
    useState<ExecutionResult | null>(null);
  const [timelineSteps, setTimelineSteps] = useState<TimelineStep[]>(
    getTimelineStepsForMode(defaultFormValues.executionMode)
  );
  const [isRunning, setIsRunning] = useState(false);
  const [statusMessage, setStatusMessage] = useState('Ready');
  const [backendConnected, setBackendConnected] = useState<boolean | null>(null);
  const [fileWritePreview, setFileWritePreview] = useState<FileWriteResponse | null>(null);
  const [canWriteFiles, setCanWriteFiles] = useState(false);
  const [testExecutionResult, setTestExecutionResult] = useState<TestExecutionResponse | null>(null);
  const [gitPrResult, setGitPrResult] = useState<GitPrResponse | null>(null);
  const [canCreateGitPr, setCanCreateGitPr] = useState(false);
  const [jiraConfigStatus, setJiraConfigStatus] = useState<JiraConfigStatus | null>(null);
  const [jiraStoryDetails, setJiraStoryDetails] = useState<JiraStoryDetails | null>(null);
  const [runHistory, setRunHistory] = useState<RunHistorySummary[]>([]);
  const [selectedRunHistory, setSelectedRunHistory] = useState<RunHistoryDetail | null>(null);
  const [runArtifacts, setRunArtifacts] = useState<RunArtifact[]>([]);
  const abortRef = useRef(false);

  useEffect(() => {
    isBackendAvailable().then(setBackendConnected);
    getJiraConfigStatus()
      .then(setJiraConfigStatus)
      .catch(() => setJiraConfigStatus(null));
    listRunHistory()
      .then(setRunHistory)
      .catch(() => setRunHistory([]));
  }, []);

  const resetTimeline = useCallback((mode = formValues.executionMode) => {
    setTimelineSteps(
      getTimelineStepsForMode(mode).map((s) => ({ ...s, status: 'pending' as const }))
    );
  }, [formValues.executionMode]);

  const animateTimelineWhileWaiting = useCallback(
    async (values: AgentFormValues) => {
      const steps = getTimelineStepsForMode(values.executionMode).map((s) => ({ ...s }));

      for (let i = 0; i < steps.length; i++) {
        if (abortRef.current) break;

        const step = steps[i];
        if (!shouldRunStep(step.label, values.executionMode)) {
          continue;
        }

        step.status = 'running';
        setTimelineSteps([...steps]);
        setStatusMessage(`${step.label}...`);

        await delay(400);

        if (abortRef.current) break;

        step.status = 'completed';
        setTimelineSteps([...steps]);

        if (shouldStopAfter(step.label, values.executionMode)) {
          break;
        }
      }
    },
    []
  );

  const simulateAgentRunLocal = useCallback(
    async (values: AgentFormValues) => {
      abortRef.current = false;
      const steps = getTimelineStepsForMode(values.executionMode).map((s) => ({ ...s }));
      setTimelineSteps(steps);
      setTestCases([]);
      setBddContent('');
      setGeneratedFiles([]);
      setSelectedFile(null);
      setRequirementSummary(null);
      setExecutionResult(null);

      const bdd = buildBddFeature(
        values.jiraStoryKey,
        values.httpMethod,
        values.endpointPath
      );
      const files = buildGeneratedFiles(
        values.jiraStoryKey,
        values.httpMethod,
        values.endpointPath
      );

      for (let i = 0; i < steps.length; i++) {
        if (abortRef.current) break;

        const step = steps[i];
        if (!shouldRunStep(step.label, values.executionMode)) {
          step.status = 'pending';
          setTimelineSteps([...steps]);
          continue;
        }

        step.status = 'running';
        setTimelineSteps([...steps]);
        setStatusMessage(`${step.label}...`);

        await delay(600 + Math.random() * 400);

        if (abortRef.current) break;

        step.status = 'completed';
        setTimelineSteps([...steps]);

        if (step.label === 'Extract Requirement') {
          setRequirementSummary(
            buildRequirementSummary(
              values.jiraStoryKey,
              values.baseApiUrl,
              values.endpointPath,
              values.httpMethod,
              values.headers
            )
          );
        }

        if (step.label === 'Generate Test Matrix') {
          setTestCases(mockTestCases);
          setActiveTab('test-case-matrix');
        }

        if (step.label === 'Generate BDD') {
          setBddContent(bdd);
          setActiveTab('generated-bdd');
        }

        if (step.label === 'Generate Automation Files') {
          setGeneratedFiles(files);
          setSelectedFile(files[0]);
          setActiveTab('generated-files');
        }

        if (step.label === 'Produce Report') {
          setExecutionResult(
            buildExecutionResult(values.httpMethod, values.endpointPath)
          );
          setActiveTab('execution-report');
        }

        if (step.label === 'Create Pull Request') {
          setStatusMessage(
            'Pull request draft ready — placeholder (PR creation not implemented).'
          );
        }

        if (shouldStopAfter(step.label, values.executionMode)) {
          break;
        }
      }

      if (!abortRef.current) {
        const completionMessages: Record<AgentFormValues['executionMode'], string> = {
          'generate-test-cases': 'Test cases generated successfully (local fallback).',
          'generate-automation': 'Automation files generated successfully (local fallback).',
          'generate-execute': 'Agent run completed successfully (local fallback).',
          'generate-execute-pr':
            'Agent run completed. PR draft placeholder ready (local fallback).',
        };
        setStatusMessage(completionMessages[values.executionMode]);
      }
    },
    []
  );

  const handleRunAgent = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before running the agent.');
      return;
    }

    abortRef.current = false;
    setIsRunning(true);
    resetTimeline(formValues.executionMode);
    setTestCases([]);
    setMatrixWarnings([]);
    setMatrixAssumptions([]);
    setBddContent('');
    setGeneratedFiles([]);
    setSelectedFile(null);
    setRequirementSummary(null);
    setExecutionResult(null);
    setStatusMessage('Running agent...');

    const request = formValuesToRequest(formValues);
    const animationPromise = animateTimelineWhileWaiting(formValues);

    try {
      const response = await runAgent(request);
      await animationPromise;

      if (abortRef.current) {
        setStatusMessage('Agent run cancelled.');
        return;
      }

      applyRunResponse(response, formValues.executionMode, {
        setRequirementSummary,
        setTestCases,
        setBddContent,
        setGeneratedFiles,
        setSelectedFile,
        setExecutionResult,
        setTimelineSteps,
        setActiveTab,
        setMatrixWarnings,
        setMatrixAssumptions,
      });

      const completionMessages: Record<AgentFormValues['executionMode'], string> = {
        'generate-test-cases': `Test cases generated via backend (run ${response.runId}).`,
        'generate-automation': `Automation generated via backend (run ${response.runId}).`,
        'generate-execute': `Agent run completed via backend (run ${response.runId}).`,
        'generate-execute-pr': `Agent run completed via backend. PR placeholder ready (run ${response.runId}).`,
      };
      setStatusMessage(completionMessages[formValues.executionMode]);
      setBackendConnected(true);
    } catch (error) {
      await animationPromise;

      if (abortRef.current) {
        setStatusMessage('Agent run cancelled.');
        return;
      }

      const message =
        error instanceof AgentApiError
          ? `Backend unavailable (${error.message}). Using local mock fallback.`
          : 'Backend unavailable. Using local mock fallback.';
      setStatusMessage(message);
      setBackendConnected(false);
      await simulateAgentRunLocal(formValues);
    } finally {
      setIsRunning(false);
    }
  };

  const handleGenerateMatrix = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors.');
      return;
    }

    setIsRunning(true);
    setStatusMessage(
      formValues.testGenerationMode === 'ai-assisted'
        ? 'Generating AI-assisted test matrix...'
        : 'Generating test matrix...'
    );

    try {
      const request = formValuesToRequest(formValues);
      const response =
        formValues.testGenerationMode === 'ai-assisted'
          ? await generateAiTestMatrix(request)
          : await generateTestMatrix(request);
      setTestCases(response.testCases);
      setMatrixWarnings(response.warnings ?? []);
      setMatrixAssumptions(response.assumptions ?? []);
      setRequirementSummary(
        buildRequirementSummary(
          formValues.jiraStoryKey,
          formValues.baseApiUrl,
          formValues.endpointPath,
          formValues.httpMethod,
          formValues.headers
        )
      );
      setActiveTab('test-case-matrix');
      const warningText = response.warnings?.length
        ? ` Warnings: ${response.warnings.join(' ')}`
        : '';
      const assumptionText = response.assumptions?.length
        ? ` Assumptions: ${response.assumptions.join(' ')}`
        : '';
      const modeLabel =
        formValues.testGenerationMode === 'ai-assisted'
          ? 'AI-assisted test matrix generated via backend.'
          : 'Test matrix generated via backend.';
      setStatusMessage(`${modeLabel}${warningText}${assumptionText}`);
      setBackendConnected(true);
    } catch {
      setTestCases(mockTestCases);
      setMatrixWarnings(['Backend unavailable. Used local mock test cases.']);
      setMatrixAssumptions([]);
      setRequirementSummary(
        buildRequirementSummary(
          formValues.jiraStoryKey,
          formValues.baseApiUrl,
          formValues.endpointPath,
          formValues.httpMethod,
          formValues.headers
        )
      );
      setActiveTab('test-case-matrix');
      setStatusMessage('Backend unavailable. Test matrix generated from local mock.');
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleExtractContract = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before extracting contract.');
      return;
    }

    setIsRunning(true);
    setApiContractError(null);
    setStatusMessage('Extracting API contract from Swagger/OpenAPI...');

    try {
      const contract = await extractContract(formValuesToRequest(formValues));
      setApiContract(contract);
      setActiveTab('api-contract');
      const warningText = contract.warnings.length
        ? ` Warnings: ${contract.warnings.join(' ')}`
        : '';
      setStatusMessage(`API contract extracted for ${contract.httpMethod} ${contract.endpointPath}.${warningText}`);
      setBackendConnected(true);
    } catch (error) {
      setApiContract(null);
      const message =
        error instanceof AgentApiError
          ? `Contract extraction failed: ${error.message}`
          : 'Contract extraction failed. Backend may be unavailable.';
      setApiContractError(message);
      setActiveTab('api-contract');
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleClear = () => {
    abortRef.current = true;
    setFormValues(defaultFormValues);
    setFormErrors({});
    setTestCases([]);
    setMatrixWarnings([]);
    setMatrixAssumptions([]);
    setAutomationWarnings([]);
    setAutomationAssumptions([]);
    setFileWritePreview(null);
    setCanWriteFiles(false);
    setTestExecutionResult(null);
    setGitPrResult(null);
    setCanCreateGitPr(false);
    setJiraStoryDetails(null);
    setRunHistory([]);
    setSelectedRunHistory(null);
    setRunArtifacts([]);
    setBddContent('');
    setGeneratedFiles([]);
    setSelectedFile(null);
    setRequirementSummary(null);
    setApiContract(null);
    setApiContractError(null);
    setExecutionResult(null);
    resetTimeline(defaultFormValues.executionMode);
    setActiveTab('requirement-summary');
    setIsRunning(false);
    setStatusMessage('Form cleared. Ready.');
  };

  const applyAutomationResponse = (
    response: AutomationGenerationResponse,
    options?: { activeTab?: WorkspaceTab }
  ) => {
    if (response.generatedBdd) {
      setBddContent(response.generatedBdd.content);
    }
    if (response.generatedFiles?.length) {
      setGeneratedFiles(response.generatedFiles);
      setSelectedFile(response.generatedFiles[0]);
    }
    setAutomationWarnings(response.warnings ?? []);
    setAutomationAssumptions(response.assumptions ?? []);

    if (options?.activeTab) {
      setActiveTab(options.activeTab);
    } else if (response.generatedFiles?.length) {
      setActiveTab('generated-files');
    } else if (response.generatedBdd) {
      setActiveTab('generated-bdd');
    }

    const warningText = response.warnings?.length
      ? ` Warnings: ${response.warnings.join(' ')}`
      : '';
    const assumptionText = response.assumptions?.length
      ? ` Assumptions: ${response.assumptions.join(' ')}`
      : '';
    const sourceLabel = response.fallbackUsed
      ? 'Automation generated via deterministic fallback.'
      : `Automation generated via AI (source: ${response.source}).`;
    setStatusMessage(`${sourceLabel}${warningText}${assumptionText}`);
  };

  const handleGenerateAutomationPackage = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before generating automation.');
      return;
    }

    setIsRunning(true);
    setAutomationWarnings([]);
    setAutomationAssumptions([]);
    setStatusMessage('Generating automation package (BDD + files)...');

    const request = buildAutomationRequest(formValues, apiContract, testCases);

    try {
      const response = await generateAiAutomationPackage(request);
      applyAutomationResponse(response);
      setBackendConnected(true);
    } catch {
      const bdd = buildBddFeature(
        formValues.jiraStoryKey,
        formValues.httpMethod,
        formValues.endpointPath
      );
      const files = buildGeneratedFiles(
        formValues.jiraStoryKey,
        formValues.httpMethod,
        formValues.endpointPath
      );
      setBddContent(bdd);
      setGeneratedFiles(files);
      setSelectedFile(files[0]);
      setAutomationWarnings(['Backend unavailable. Used local mock automation scaffold.']);
      setAutomationAssumptions([]);
      setActiveTab('generated-files');
      setStatusMessage(
        'Backend unavailable. Automation package generated from local mock.'
      );
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleGenerateAiBdd = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before generating BDD.');
      return;
    }

    setIsRunning(true);
    setAutomationWarnings([]);
    setAutomationAssumptions([]);
    setStatusMessage('Generating BDD from test matrix...');

    const request = buildAutomationRequest(formValues, apiContract, testCases);

    try {
      const response = await generateAiBdd(request);
      applyAutomationResponse(response, { activeTab: 'generated-bdd' });
      setBackendConnected(true);
    } catch {
      setBddContent(
        buildBddFeature(
          formValues.jiraStoryKey,
          formValues.httpMethod,
          formValues.endpointPath
        )
      );
      setAutomationWarnings(['Backend unavailable. Used local mock BDD.']);
      setAutomationAssumptions([]);
      setActiveTab('generated-bdd');
      setStatusMessage('Backend unavailable. BDD generated from local mock.');
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleGenerateAiFiles = async () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before generating files.');
      return;
    }

    setIsRunning(true);
    setAutomationWarnings([]);
    setAutomationAssumptions([]);
    setStatusMessage('Generating automation files...');

    const request = buildAutomationRequest(formValues, apiContract, testCases);

    try {
      const response = await generateAiFiles(request);
      applyAutomationResponse(response, { activeTab: 'generated-files' });
      setBackendConnected(true);
    } catch {
      const files = buildGeneratedFiles(
        formValues.jiraStoryKey,
        formValues.httpMethod,
        formValues.endpointPath
      );
      setGeneratedFiles(files);
      setSelectedFile(files[0]);
      setAutomationWarnings(['Backend unavailable. Used local mock automation files.']);
      setAutomationAssumptions([]);
      setActiveTab('generated-files');
      setStatusMessage('Backend unavailable. Automation files generated from local mock.');
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleRegenerateBdd = async () => {
    setIsRunning(true);
    setStatusMessage('Regenerating BDD...');

    try {
      const bdd = await generateBdd(formValuesToRequest(formValues));
      setBddContent(bdd.content);
      setStatusMessage('BDD feature file regenerated via backend.');
      setBackendConnected(true);
    } catch {
      setBddContent(
        buildBddFeature(
          formValues.jiraStoryKey,
          formValues.httpMethod,
          formValues.endpointPath
        )
      );
      setStatusMessage('Backend unavailable. BDD regenerated from local mock.');
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const updateWriteEligibility = (response: FileWriteResponse) => {
    const blocked = response.summary.blocked > 0 || response.summary.errors > 0;
    const hasErrors = response.errors.length > 0;
    setCanWriteFiles(!blocked && !hasErrors);
  };

  const handlePreviewWrite = async () => {
    if (!formValues.projectPath.trim()) {
      setStatusMessage('Project path is required before previewing file writes.');
      return;
    }
    if (generatedFiles.length === 0) {
      setStatusMessage('Generate automation files before previewing a write.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Previewing file write to project...');

    try {
      const response = await previewFileWrite(
        buildFileWriteRequest(formValues, generatedFiles)
      );
      setFileWritePreview(response);
      updateWriteEligibility(response);
      setActiveTab('file-write-preview');
      const warningText = response.warnings.length
        ? ` Warnings: ${response.warnings.join(' ')}`
        : '';
      const errorText = response.errors.length ? ` Errors: ${response.errors.join(' ')}` : '';
      setStatusMessage(
        `File write preview ready (${response.summary.create} create, ${response.summary.update} update, ${response.summary.skip} skip, ${response.summary.blocked} blocked).${warningText}${errorText}`
      );
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `File write preview failed: ${error.message}`
          : 'File write preview failed. Backend may be unavailable.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleWriteFiles = async () => {
    if (!canWriteFiles || !fileWritePreview) {
      setStatusMessage('Preview file writes first and resolve blocked/errors before writing.');
      return;
    }
    if (!formValues.projectPath.trim() || generatedFiles.length === 0) {
      setStatusMessage('Project path and generated files are required before writing.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Writing generated files to project...');

    try {
      const response = await writeGeneratedFiles(
        buildFileWriteRequest(formValues, generatedFiles)
      );
      setFileWritePreview(response);
      updateWriteEligibility(response);
      setActiveTab('file-write-preview');
      setStatusMessage(
        `File write completed (${response.summary.written} written, ${response.summary.skip} skipped, ${response.summary.blocked} blocked, ${response.summary.errors} errors).`
      );
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `File write failed: ${error.message}`
          : 'File write failed. Backend may be unavailable.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handlePreviewTestExecution = async () => {
    if (!formValues.projectPath.trim()) {
      setStatusMessage('Project path is required before previewing test execution.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Previewing Maven test execution...');

    try {
      const response = await previewTestExecution(
        buildTestExecutionRequest(formValues, environment)
      );
      setTestExecutionResult(response);
      setActiveTab('test-execution');
      setStatusMessage(`Test execution preview ready (${response.status}). Command: ${response.command}`);
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Test execution preview failed: ${error.message}`
          : 'Test execution preview failed. Backend may be unavailable.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleRunTestExecution = async () => {
    if (!formValues.projectPath.trim()) {
      setStatusMessage('Project path is required before running Maven tests.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Running Maven tests...');

    try {
      const response = await runTestExecution(
        buildTestExecutionRequest(formValues, environment)
      );
      setTestExecutionResult(response);
      setActiveTab('test-execution');
      setStatusMessage(
        `Maven test execution completed with status ${response.status} (execution ${response.executionId}).`
      );
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Maven test execution failed: ${error.message}`
          : 'Maven test execution failed. Backend may be unavailable.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const updateGitPrEligibility = (response: GitPrResponse) => {
    setCanCreateGitPr(
      response.status === 'READY'
          && response.errors.length === 0
          && response.changedFiles.length > 0
    );
  };

  const handlePreviewGitPr = async () => {
    if (!formValues.projectPath.trim()) {
      setStatusMessage('Project path is required before previewing Git PR.');
      return;
    }

    const filesToCommit = deriveFilesToCommit(generatedFiles, fileWritePreview);
    if (filesToCommit.length === 0) {
      setStatusMessage('Generate and write automation files before previewing a Git PR.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Previewing Git branch and PR...');

    try {
      const response = await previewGitPr(
        buildGitPrRequest(formValues, filesToCommit)
      );
      setGitPrResult(response);
      updateGitPrEligibility(response);
      setActiveTab('git-pr');
      setStatusMessage(`Git PR preview ready (${response.status}).`);
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Git PR preview failed: ${error.message}`
          : 'Git PR preview failed. Backend may be unavailable.';
      setStatusMessage(message);
      setCanCreateGitPr(false);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleCreateGitPr = async () => {
    if (!canCreateGitPr || !gitPrResult) {
      setStatusMessage('Preview Git PR first and resolve errors before creating a pull request.');
      return;
    }

    const filesToCommit = deriveFilesToCommit(generatedFiles, fileWritePreview);
    if (!formValues.projectPath.trim() || filesToCommit.length === 0) {
      setStatusMessage('Project path and files to commit are required before creating a PR.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Creating branch, commit, and pull request...');

    try {
      const response = await createGitPr(
        buildGitPrRequest(formValues, filesToCommit)
      );
      setGitPrResult(response);
      updateGitPrEligibility(response);
      setActiveTab('git-pr');
      setStatusMessage(
        response.prUrl
          ? `Pull request created: ${response.prUrl}`
          : `Git PR operation completed with status ${response.status}.`
      );
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Git PR creation failed: ${error.message}`
          : 'Git PR creation failed. Backend may be unavailable.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleFetchJiraStory = async () => {
    if (!formValues.jiraStoryKey.trim()) {
      setStatusMessage('Jira story key is required before fetching from Jira.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Fetching Jira story...');

    try {
      const story = await fetchJiraStory(formValues.jiraStoryKey.trim());
      setJiraStoryDetails(story);
      setFormValues((current) => ({
        ...current,
        jiraStoryKey: story.jiraStoryKey,
        jiraStoryText: buildJiraStoryText(story),
      }));
      setRequirementSummary({
        jiraKey: story.jiraStoryKey,
        endpoint: formValues.endpointPath || '(not set)',
        method: formValues.httpMethod,
        requiredHeaders: formValues.headers.map((h) => h.key).filter(Boolean),
        requestBodyFields: [],
        expectedStatusCodes: [],
        businessRules: story.acceptanceCriteria,
        assumptions: story.warnings,
      });
      const warningText = story.warnings.length ? ` Warnings: ${story.warnings.join(' ')}` : '';
      setStatusMessage(`Jira story ${story.jiraStoryKey} fetched.${warningText}`);
      setBackendConnected(true);
      getJiraConfigStatus().then(setJiraConfigStatus).catch(() => undefined);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Jira fetch failed: ${error.message}`
          : 'Jira fetch failed. Backend may be unavailable or Jira is disabled.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handlePostJiraSummary = async () => {
    if (!formValues.jiraStoryKey.trim()) {
      setStatusMessage('Jira story key is required before posting a summary.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Posting summary to Jira...');

    try {
      const response = await postJiraSummary(
        buildJiraPostSummaryRequest(
          formValues.jiraStoryKey.trim(),
          testCases,
          bddContent,
          fileWritePreview,
          testExecutionResult,
          gitPrResult
        )
      );
      const errorText = response.errors.length ? ` Errors: ${response.errors.join(' ')}` : '';
      setStatusMessage(`${response.message ?? response.status}${errorText}`);
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Jira summary post failed: ${error.message}`
          : 'Jira summary post failed. Backend may be unavailable or Jira is disabled.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleLinkPrToJira = async () => {
    if (!formValues.jiraStoryKey.trim()) {
      setStatusMessage('Jira story key is required before linking a PR.');
      return;
    }
    if (!gitPrResult?.prUrl) {
      setStatusMessage('Create a pull request first, then link it to Jira.');
      return;
    }

    setIsRunning(true);
    setStatusMessage('Linking pull request to Jira...');

    try {
      const response = await linkPrToJira({
        jiraStoryKey: formValues.jiraStoryKey.trim(),
        prUrl: gitPrResult.prUrl,
      });
      const errorText = response.errors.length ? ` Errors: ${response.errors.join(' ')}` : '';
      setStatusMessage(`${response.message ?? response.status}${errorText}`);
      setBackendConnected(true);
    } catch (error) {
      const message =
        error instanceof AgentApiError
          ? `Jira PR link failed: ${error.message}`
          : 'Jira PR link failed. Backend may be unavailable or Jira is disabled.';
      setStatusMessage(message);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const refreshRunHistory = async () => {
    try {
      const runs = await listRunHistory();
      setRunHistory(runs);
      setBackendConnected(true);
    } catch {
      setStatusMessage('Failed to load run history.');
      setBackendConnected(false);
    }
  };

  const handleRefreshRunHistory = async () => {
    setIsRunning(true);
    setStatusMessage('Refreshing run history...');
    try {
      const runs = await listRunHistory();
      setRunHistory(runs);
      setActiveTab('run-history');
      setStatusMessage(`Loaded ${runs.length} persisted runs.`);
      setBackendConnected(true);
    } catch {
      setStatusMessage('Failed to load run history.');
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleOpenRunHistory = async (runId: string) => {
    setIsRunning(true);
    try {
      const detail = await getRunHistory(runId);
      setSelectedRunHistory(detail);
      setActiveTab('run-history');
      setStatusMessage(`Opened run ${runId}.`);
      setBackendConnected(true);
    } catch {
      setStatusMessage(`Failed to open run ${runId}.`);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleLoadRunArtifacts = async (runId: string) => {
    setIsRunning(true);
    try {
      const artifacts = await getRunArtifacts(runId);
      setRunArtifacts(artifacts);
      setActiveTab('run-history');
      setStatusMessage(`Loaded ${artifacts.length} artifacts for run ${runId}.`);
      setBackendConnected(true);
    } catch {
      setStatusMessage(`Failed to load artifacts for run ${runId}.`);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handleDeleteRunHistory = async (runId: string) => {
    setIsRunning(true);
    try {
      await deleteRunHistory(runId);
      if (selectedRunHistory?.runId === runId) {
        setSelectedRunHistory(null);
        setRunArtifacts([]);
      }
      await refreshRunHistory();
      setStatusMessage(`Deleted run ${runId}.`);
      setBackendConnected(true);
    } catch {
      setStatusMessage(`Failed to delete run ${runId}.`);
      setBackendConnected(false);
    } finally {
      setIsRunning(false);
    }
  };

  const handlePlaceholder = (action: string) => {
    setStatusMessage(`${action} — placeholder action (not implemented).`);
  };

  const footerMessage =
    backendConnected === null
      ? statusMessage
      : backendConnected
        ? `${statusMessage} | Backend: connected`
        : `${statusMessage} | Backend: offline (using fallback)`;

  return (
    <div className={`app ${theme === 'dark' ? 'app--dark' : ''}`}>
      <Header
        environment={environment}
        onEnvironmentChange={setEnvironment}
        theme={theme}
        onThemeToggle={() => setTheme((t) => (t === 'light' ? 'dark' : 'light'))}
      />

      <main className="app-main">
        <AgentInputPanel
          values={formValues}
          errors={formErrors}
          isRunning={isRunning}
          jiraConfigStatus={jiraConfigStatus}
          jiraStoryDetails={jiraStoryDetails}
          onChange={(values) => {
            const nextValues = applyJiraGitDefaults(values);
            setFormValues(nextValues);
            if (values.executionMode !== formValues.executionMode && !isRunning) {
              resetTimeline(values.executionMode);
            }
          }}
          onRunAgent={handleRunAgent}
          onGenerateMatrix={handleGenerateMatrix}
          onExtractContract={handleExtractContract}
          onGenerateAutomationPackage={handleGenerateAutomationPackage}
          onFetchJiraStory={handleFetchJiraStory}
          onPostJiraSummary={handlePostJiraSummary}
          onLinkPrToJira={handleLinkPrToJira}
          onClear={handleClear}
        />

        <div className="workspace-area">
          <WorkspaceTabs
            activeTab={activeTab}
            onTabChange={setActiveTab}
            requirementSummary={requirementSummary}
            apiContract={apiContract}
            apiContractError={apiContractError}
            testCases={testCases}
            matrixWarnings={matrixWarnings}
            matrixAssumptions={matrixAssumptions}
            automationWarnings={automationWarnings}
            automationAssumptions={automationAssumptions}
            bddContent={bddContent}
            bddDownloadFilename={bddDownloadFilename(formValues.endpointPath)}
            generatedFiles={generatedFiles}
            selectedFile={selectedFile}
            onSelectFile={setSelectedFile}
            executionResult={executionResult}
            onRegenerateBdd={handleRegenerateBdd}
            onGenerateAiBdd={handleGenerateAiBdd}
            onGenerateAiFiles={handleGenerateAiFiles}
            onPreviewWrite={handlePreviewWrite}
            onWriteFiles={handleWriteFiles}
            fileWritePreview={fileWritePreview}
            canWriteFiles={canWriteFiles}
            isRunning={isRunning}
            testExecutionResult={testExecutionResult}
            onPreviewTestExecution={handlePreviewTestExecution}
            onRunTestExecution={handleRunTestExecution}
            gitPrResult={gitPrResult}
            canCreateGitPr={canCreateGitPr}
            onPreviewGitPr={handlePreviewGitPr}
            onCreateGitPr={handleCreateGitPr}
            runHistory={runHistory}
            selectedRunHistory={selectedRunHistory}
            runArtifacts={runArtifacts}
            onRefreshRunHistory={handleRefreshRunHistory}
            onOpenRunHistory={handleOpenRunHistory}
            onLoadRunArtifacts={handleLoadRunArtifacts}
            onDeleteRunHistory={handleDeleteRunHistory}
            onCreateBugDraft={() => handlePlaceholder('Create Bug Draft')}
            onRerunFailed={() => handlePlaceholder('Re-run Failed Tests')}
            onExportReport={() => handlePlaceholder('Export Report')}
          />
          <AgentTimeline steps={timelineSteps} />
        </div>
      </main>

      <FooterStatusBar message={footerMessage} isRunning={isRunning} />
    </div>
  );
}
