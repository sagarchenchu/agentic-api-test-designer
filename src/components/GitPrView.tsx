import type { GitPrResponse } from '../types';
import CodeBlock from './CodeBlock';
import StatusBadge from './StatusBadge';

interface GitPrViewProps {
  result: GitPrResponse | null;
  isRunning: boolean;
  canCreatePr: boolean;
  onPreview: () => void;
  onCreate: () => void;
}

function statusVariant(
  status: string
): 'success' | 'warning' | 'error' | 'info' | 'neutral' | 'running' {
  switch (status) {
    case 'READY':
    case 'CREATED':
      return 'success';
    case 'FAILED':
      return 'warning';
    case 'ERROR':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function GitPrView({
  result,
  isRunning,
  canCreatePr,
  onPreview,
  onCreate,
}: GitPrViewProps) {
  return (
    <div className="git-pr-view">
      <div className="action-bar">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onPreview}
          disabled={isRunning}
        >
          Preview Git PR
        </button>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={onCreate}
          disabled={isRunning || !canCreatePr}
          title={
            canCreatePr
              ? 'Create branch, commit, push, and open GitHub PR'
              : result?.status === 'READY' && result.changedFiles.length === 0
                ? 'No changed files in git status — commit would likely fail with nothing to commit'
                : 'Preview Git PR first and resolve errors before creating a PR'
          }
        >
          {isRunning ? 'Creating Pull Request...' : 'Create Pull Request'}
        </button>
      </div>

      {!result ? (
        <div className="empty-state">
          <p>
            Write generated automation files to the project, then preview branch/commit/PR
            details before creating a pull request.
          </p>
        </div>
      ) : (
        <>
          <div className="file-write-summary card-inline">
            <div className="test-execution-header">
              <h3>Git / PR Result</h3>
              <StatusBadge label={result.status} variant={statusVariant(result.status)} />
            </div>
            <div className="summary-grid">
              <span>Operation ID: {result.operationId}</span>
              <span>Base Branch: {result.baseBranch}</span>
              <span>New Branch: {result.newBranchName}</span>
              <span>Commit SHA: {result.commitSha ?? '—'}</span>
            </div>
            {result.prUrl && (
              <p>
                PR URL:{' '}
                <a href={result.prUrl} target="_blank" rel="noreferrer">
                  {result.prUrl}
                </a>
              </p>
            )}
            <p className="mono file-write-project-path">{result.projectPath}</p>
          </div>

          {result.changedFiles.length > 0 && (
            <div className="card-inline">
              <h4>Files to Commit</h4>
              <ul>
                {result.changedFiles.map((file) => (
                  <li key={file} className="mono">
                    {file}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {result.warnings.length > 0 && (
            <div className="contract-warnings matrix-meta">
              <h4>Warnings</h4>
              <ul>
                {result.warnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </div>
          )}

          {result.errors.length > 0 && (
            <div className="contract-warnings matrix-meta">
              <h4>Errors</h4>
              <ul>
                {result.errors.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            </div>
          )}

          {result.commandLog.length > 0 && (
            <div className="file-write-diff">
              <h4>Command Log</h4>
              <CodeBlock code={result.commandLog.join('\n')} language="shell" />
            </div>
          )}
        </>
      )}
    </div>
  );
}
