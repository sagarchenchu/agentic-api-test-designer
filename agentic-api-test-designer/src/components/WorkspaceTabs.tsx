import type { WorkspaceTab } from '../types';
import RequirementSummaryView from './RequirementSummary';
import TestCaseMatrix from './TestCaseMatrix';
import GeneratedBddPreview from './GeneratedBddPreview';
import GeneratedFilesTree from './GeneratedFilesTree';
import ExecutionReport from './ExecutionReport';
import type {
  RequirementSummary,
  TestCase,
  GeneratedFile,
  ExecutionResult,
} from '../types';

const TABS: { id: WorkspaceTab; label: string }[] = [
  { id: 'requirement-summary', label: 'Requirement Summary' },
  { id: 'test-case-matrix', label: 'Test Case Matrix' },
  { id: 'generated-bdd', label: 'Generated BDD' },
  { id: 'generated-files', label: 'Generated Files' },
  { id: 'execution-report', label: 'Execution Report' },
];

interface WorkspaceTabsProps {
  activeTab: WorkspaceTab;
  onTabChange: (tab: WorkspaceTab) => void;
  requirementSummary: RequirementSummary | null;
  testCases: TestCase[];
  bddContent: string;
  generatedFiles: GeneratedFile[];
  selectedFile: GeneratedFile | null;
  onSelectFile: (file: GeneratedFile) => void;
  executionResult: ExecutionResult | null;
  onRegenerateBdd: () => void;
  onCreateBugDraft: () => void;
  onRerunFailed: () => void;
  onExportReport: () => void;
}

export default function WorkspaceTabs({
  activeTab,
  onTabChange,
  requirementSummary,
  testCases,
  bddContent,
  generatedFiles,
  selectedFile,
  onSelectFile,
  executionResult,
  onRegenerateBdd,
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
        {activeTab === 'test-case-matrix' && (
          <TestCaseMatrix testCases={testCases} />
        )}
        {activeTab === 'generated-bdd' && (
          <GeneratedBddPreview
            featureContent={bddContent}
            onRegenerate={onRegenerateBdd}
          />
        )}
        {activeTab === 'generated-files' && (
          <GeneratedFilesTree
            files={generatedFiles}
            selectedFile={selectedFile}
            onSelectFile={onSelectFile}
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
