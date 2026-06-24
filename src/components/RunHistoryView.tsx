import type { RunArtifact, RunHistoryDetail, RunHistorySummary } from '../types';
import StatusBadge from './StatusBadge';

interface RunHistoryViewProps {
  runs: RunHistorySummary[];
  selectedRun: RunHistoryDetail | null;
  artifacts: RunArtifact[];
  isRunning: boolean;
  onRefresh: () => void;
  onOpenRun: (runId: string) => void;
  onLoadArtifacts: (runId: string) => void;
  onDeleteRun: (runId: string) => void;
}

function statusVariant(status: string): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (status?.toUpperCase()) {
    case 'COMPLETED':
    case 'CREATED':
    case 'SUCCESS':
    case 'PASSED':
    case 'RECORDED':
      return 'success';
    case 'FAILED':
    case 'ERROR':
      return 'error';
    case 'RUNNING':
      return 'info';
    default:
      return 'neutral';
  }
}

function formatTime(value?: string | null): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
}

export default function RunHistoryView({
  runs,
  selectedRun,
  artifacts,
  isRunning,
  onRefresh,
  onOpenRun,
  onLoadArtifacts,
  onDeleteRun,
}: RunHistoryViewProps) {
  return (
    <div className="run-history-view">
      <div className="action-bar">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onRefresh}
          disabled={isRunning}
        >
          Refresh Run History
        </button>
      </div>

      {runs.length === 0 ? (
        <div className="empty-state">
          <p>No persisted runs yet. Execute the agent or run file write, tests, Git PR, or Jira updates.</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Run ID</th>
                <th>Jira</th>
                <th>Status</th>
                <th>Created</th>
                <th>Tests</th>
                <th>Files</th>
                <th>Execution</th>
                <th>PR</th>
                <th>Jira Update</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {runs.map((run) => (
                <tr key={run.runId}>
                  <td className="mono">{run.runId.slice(0, 8)}…</td>
                  <td>{run.jiraStoryKey ?? '—'}</td>
                  <td>
                    <StatusBadge label={run.status} variant={statusVariant(run.status)} />
                  </td>
                  <td>{formatTime(run.createdAt)}</td>
                  <td>{run.testCaseCount}</td>
                  <td>{run.generatedFileCount}</td>
                  <td>{run.testExecutionStatus ?? '—'}</td>
                  <td>
                    {run.gitPrUrl ? (
                      <a href={run.gitPrUrl} target="_blank" rel="noreferrer">
                        PR
                      </a>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{run.jiraUpdateStatus ?? '—'}</td>
                  <td className="run-history-actions">
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => onOpenRun(run.runId)}>
                      Open
                    </button>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => onLoadArtifacts(run.runId)}>
                      Artifacts
                    </button>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => onDeleteRun(run.runId)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedRun && (
        <div className="card-inline">
          <h3>Run Detail: {selectedRun.runId}</h3>
          <div className="summary-grid">
            <span>Mode: {selectedRun.executionMode ?? '—'}</span>
            <span>Framework: {selectedRun.frameworkType ?? '—'}</span>
            <span>Swagger Source: {selectedRun.swaggerSourceType ?? '—'}</span>
            <span>File Write: {selectedRun.fileWriteSummary ?? '—'}</span>
          </div>
          {selectedRun.warnings.length > 0 && (
            <div>
              <h4>Warnings</h4>
              <ul>
                {selectedRun.warnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </div>
          )}
          {selectedRun.errors.length > 0 && (
            <div>
              <h4>Errors</h4>
              <ul>
                {selectedRun.errors.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {artifacts.length > 0 && (
        <div className="card-inline">
          <h3>Artifacts</h3>
          <ul>
            {artifacts.map((artifact) => (
              <li key={artifact.id}>
                <strong>{artifact.artifactType}</strong>: {artifact.name}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
