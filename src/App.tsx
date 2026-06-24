import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  AgentFormValues,
  ApiContract,
  Environment,
  ExecutionResult,
  FormErrors,
  GeneratedFile,
  RequirementSummary,
  TestCase,
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
  extractContract,
  generateBdd,
  generateTestMatrix,
  generateAiTestMatrix,
  runAgent,
  isBackendAvailable,
  AgentApiError,
} from './api/agentApi';
import type { AgentRunResponse } from './api/agentApi';
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
  const abortRef = useRef(false);

  useEffect(() => {
    isBackendAvailable().then(setBackendConnected);
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
          onChange={(values) => {
            setFormValues(values);
            if (values.executionMode !== formValues.executionMode && !isRunning) {
              resetTimeline(values.executionMode);
            }
          }}
          onRunAgent={handleRunAgent}
          onGenerateMatrix={handleGenerateMatrix}
          onExtractContract={handleExtractContract}
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
            bddContent={bddContent}
            bddDownloadFilename={bddDownloadFilename(formValues.endpointPath)}
            generatedFiles={generatedFiles}
            selectedFile={selectedFile}
            onSelectFile={setSelectedFile}
            executionResult={executionResult}
            onRegenerateBdd={handleRegenerateBdd}
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
