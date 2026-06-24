import { useCallback, useRef, useState } from 'react';
import type {
  AgentFormValues,
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

export default function App() {
  const [environment, setEnvironment] = useState<Environment>('QA');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [formValues, setFormValues] = useState<AgentFormValues>(defaultFormValues);
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [activeTab, setActiveTab] = useState<WorkspaceTab>('requirement-summary');
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [bddContent, setBddContent] = useState('');
  const [generatedFiles, setGeneratedFiles] = useState<GeneratedFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<GeneratedFile | null>(null);
  const [requirementSummary, setRequirementSummary] =
    useState<RequirementSummary | null>(null);
  const [executionResult, setExecutionResult] =
    useState<ExecutionResult | null>(null);
  const [timelineSteps, setTimelineSteps] = useState<TimelineStep[]>(
    getTimelineStepsForMode(defaultFormValues.executionMode)
  );
  const [isRunning, setIsRunning] = useState(false);
  const [statusMessage, setStatusMessage] = useState('Ready');
  const abortRef = useRef(false);

  const resetTimeline = useCallback((mode = formValues.executionMode) => {
    setTimelineSteps(
      getTimelineStepsForMode(mode).map((s) => ({ ...s, status: 'pending' as const }))
    );
  }, [formValues.executionMode]);

  const simulateAgentRun = useCallback(
    async (values: AgentFormValues) => {
      abortRef.current = false;
      setIsRunning(true);
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

      setIsRunning(false);
      if (!abortRef.current) {
        const completionMessages: Record<AgentFormValues['executionMode'], string> = {
          'generate-test-cases': 'Test cases generated successfully.',
          'generate-automation': 'Automation files generated successfully.',
          'generate-execute': 'Agent run completed successfully.',
          'generate-execute-pr':
            'Agent run completed. PR draft placeholder ready (not implemented).',
        };
        setStatusMessage(completionMessages[values.executionMode]);
      } else {
        setStatusMessage('Agent run cancelled.');
      }
    },
    []
  );

  const handleRunAgent = () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors before running the agent.');
      return;
    }
    resetTimeline(formValues.executionMode);
    simulateAgentRun(formValues);
  };

  const handleGenerateMatrix = () => {
    const errors = validateForm(formValues);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage('Please fix validation errors.');
      return;
    }
    setTestCases(mockTestCases);
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
    setStatusMessage('Test matrix generated from inputs.');
  };

  const handleClear = () => {
    abortRef.current = true;
    setFormValues(defaultFormValues);
    setFormErrors({});
    setTestCases([]);
    setBddContent('');
    setGeneratedFiles([]);
    setSelectedFile(null);
    setRequirementSummary(null);
    setExecutionResult(null);
    resetTimeline(defaultFormValues.executionMode);
    setActiveTab('requirement-summary');
    setIsRunning(false);
    setStatusMessage('Form cleared. Ready.');
  };

  const handleRegenerateBdd = () => {
    setBddContent(
      buildBddFeature(
        formValues.jiraStoryKey,
        formValues.httpMethod,
        formValues.endpointPath
      )
    );
    setStatusMessage('BDD feature file regenerated from current inputs.');
  };

  const handlePlaceholder = (action: string) => {
    setStatusMessage(`${action} — placeholder action (not implemented).`);
  };

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
          onClear={handleClear}
        />

        <div className="workspace-area">
          <WorkspaceTabs
            activeTab={activeTab}
            onTabChange={setActiveTab}
            requirementSummary={requirementSummary}
            testCases={testCases}
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

      <FooterStatusBar message={statusMessage} isRunning={isRunning} />
    </div>
  );
}
