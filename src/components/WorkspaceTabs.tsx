import type { WorkspaceTab } from '../types';
import RequirementSummaryView from './RequirementSummary';
import ApiContractView from './ApiContractView';
import TestCaseMatrix from './TestCaseMatrix';
import GeneratedBddPreview from './GeneratedBddPreview';
import GeneratedFilesTree from './GeneratedFilesTree';
import FileWritePreview from './FileWritePreview';
import ExecutionReport from './ExecutionReport';
import type {
  ApiContract,
  RequirementSummary,
  TestCase,
  GeneratedFile,
  ExecutionResult,
  FileWriteResponse,
} from '../types';

const TABS: { id: WorkspaceTab; label: string }[] = [
  { id: 'requirement-summary', label: 'Requirement Summary' },
  { id: 'api-contract', label: 'API Contract' },
  { id: 'test-case-matrix', label: 'Test Case Matrix' },
  { id: 'generated-bdd', label: 'Generated BDD' },
  { id: 'generated-files', label: 'Generated Files' },
  { id: 'file-write-preview', label: 'File Write Preview' },
  { id: 'execution-report', label: 'Execution Report' },
];

interface WorkspaceTabsProps {
  activeTab: WorkspaceTab;
  onTabChange: (tab: WorkspaceTab) => void;
  requirementSummary: RequirementSummary | null;
  apiContract: ApiContract | null;
  apiContractError: string | null;
  testCases: TestCase[];
  matrixWarnings?: string[];
  matrixAssumptions?: string[];
  automationWarnings?: string[];
  automationAssumptions?: string[];
  bddContent: string;
  bddDownloadFilename?: string;
  generatedFiles: GeneratedFile[];
  selectedFile: GeneratedFile | null;
  onSelectFile: (file: GeneratedFile) => void;
  executionResult: ExecutionResult | null;
  onRegenerateBdd: () => void;
  onGenerateAiBdd: () => void;
  onGenerateAiFiles: () => void;
  onPreviewWrite: () => void;
  onWriteFiles: () => void;
  fileWritePreview: FileWriteResponse | null;
  canWriteFiles: boolean;
  isRunning: boolean;
  onCreateBugDraft: () => void;
  onRerunFailed: () => void;
  onExportReport: () => void;
}

export default function WorkspaceTabs({
  activeTab,
  onTabChange,
  requirementSummary,
  apiContract,
  apiContractError,
  testCases,
  matrixWarnings,
  matrixAssumptions,
  automationWarnings,
  automationAssumptions,
  bddContent,
  bddDownloadFilename,
  generatedFiles,
  selectedFile,
  onSelectFile,
  executionResult,
  onRegenerateBdd,
  onGenerateAiBdd,
  onGenerateAiFiles,
  onPreviewWrite,
  onWriteFiles,
  fileWritePreview,
  canWriteFiles,
  isRunning,
  onCreateBugDraft,
  onRerunFailed,
  onExportReport,
}: WorkspaceTabsProps) {
  return (
    <div className="workspace-tabs card">
      <div className="tab-bar" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={`tab-button ${activeTab === tab.id ? 'tab-button--active' : ''}`}
            onClick={() => onTabChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="tab-content" role="tabpanel">
        {activeTab === 'requirement-summary' && (
          <RequirementSummaryView summary={requirementSummary} />
        )}
        {activeTab === 'api-contract' && (
          <ApiContractView contract={apiContract} error={apiContractError} />
        )}
        {activeTab === 'test-case-matrix' && (
          <TestCaseMatrix
            testCases={testCases}
            warnings={matrixWarnings}
            assumptions={matrixAssumptions}
          />
        )}
        {activeTab === 'generated-bdd' && (
          <GeneratedBddPreview
            featureContent={bddContent}
            downloadFilename={bddDownloadFilename}
            warnings={automationWarnings}
            assumptions={automationAssumptions}
            onRegenerate={onRegenerateBdd}
            onGenerateFromMatrix={onGenerateAiBdd}
          />
        )}
        {activeTab === 'generated-files' && (
          <GeneratedFilesTree
            files={generatedFiles}
            selectedFile={selectedFile}
            onSelectFile={onSelectFile}
            warnings={automationWarnings}
            assumptions={automationAssumptions}
            onGenerateFiles={onGenerateAiFiles}
            onPreviewWrite={onPreviewWrite}
            onWriteFiles={onWriteFiles}
            canWriteFiles={canWriteFiles}
            isRunning={isRunning}
          />
        )}
        {activeTab === 'file-write-preview' && (
          <FileWritePreview
            preview={fileWritePreview}
            canWrite={canWriteFiles}
            isRunning={isRunning}
            onWriteFiles={onWriteFiles}
          />
        )}
        {activeTab === 'execution-report' && (
          <ExecutionReport
            result={executionResult}
            onCreateBugDraft={onCreateBugDraft}
            onRerunFailed={onRerunFailed}
            onExportReport={onExportReport}
          />
        )}
      </div>
    </div>
  );
}
