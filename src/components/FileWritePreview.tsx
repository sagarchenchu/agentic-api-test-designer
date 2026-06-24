import { useState } from 'react';
import type { FileWriteResponse } from '../types';
import CodeBlock from './CodeBlock';
import StatusBadge from './StatusBadge';

interface FileWritePreviewProps {
  preview: FileWriteResponse | null;
  canWrite: boolean;
  isRunning: boolean;
  onWriteFiles: () => void;
}

function actionVariant(action: string): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (action) {
    case 'CREATE':
      return 'success';
    case 'UPDATE':
      return 'info';
    case 'SKIP':
      return 'warning';
    case 'BLOCKED':
      return 'error';
    default:
      return 'neutral';
  }
}

function statusVariant(status: string): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (status) {
    case 'WRITTEN':
    case 'READY':
      return 'success';
    case 'SKIPPED':
      return 'warning';
    case 'BLOCKED':
    case 'ERROR':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function FileWritePreview({
  preview,
  canWrite,
  isRunning,
  onWriteFiles,
}: FileWritePreviewProps) {
  const [expandedDiffs, setExpandedDiffs] = useState<Record<string, boolean>>({});

  if (!preview) {
    return (
      <div className="empty-state">
        <p>
          Generate automation files, then use Preview Write to Project from the Generated Files tab.
        </p>
      </div>
    );
  }

  const toggleDiff = (path: string) => {
    setExpandedDiffs((current) => ({ ...current, [path]: !current[path] }));
  };

  return (
    <div className="file-write-preview">
      <div className="action-bar">
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={onWriteFiles}
          disabled={!canWrite || isRunning}
          title={
            canWrite
              ? 'Write previewed files to the project path'
              : 'Run preview first and resolve blocked/errors before writing'
          }
        >
          {isRunning ? 'Writing...' : 'Write Files to Project'}
        </button>
      </div>

      <div className="file-write-summary card-inline">
        <h3>Write Summary</h3>
        <div className="summary-grid">
          <span>Total: {preview.summary.total}</span>
          <span>Create: {preview.summary.create}</span>
          <span>Update: {preview.summary.update}</span>
          <span>Skip: {preview.summary.skip}</span>
          <span>Blocked: {preview.summary.blocked}</span>
          <span>Written: {preview.summary.written}</span>
          <span>Errors: {preview.summary.errors}</span>
        </div>
        <p className="mono file-write-project-path">{preview.projectPath}</p>
      </div>

      {preview.warnings.length > 0 && (
        <div className="contract-warnings matrix-meta">
          <h4>Warnings</h4>
          <ul>
            {preview.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      {preview.errors.length > 0 && (
        <div className="contract-warnings matrix-meta">
          <h4>Errors</h4>
          <ul>
            {preview.errors.map((error) => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Relative Path</th>
              <th>Action</th>
              <th>Status</th>
              <th>Message</th>
              <th>Diff</th>
            </tr>
          </thead>
          <tbody>
            {preview.results.map((result) => (
              <tr key={result.relativePath}>
                <td className="mono">{result.relativePath}</td>
                <td>
                  <StatusBadge label={result.action} variant={actionVariant(result.action)} />
                </td>
                <td>
                  <StatusBadge label={result.status} variant={statusVariant(result.status)} />
                </td>
                <td>{result.message}</td>
                <td>
                  {result.diff ? (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => toggleDiff(result.relativePath)}
                    >
                      {expandedDiffs[result.relativePath] ? 'Hide Diff' : 'Show Diff'}
                    </button>
                  ) : (
                    '—'
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {preview.results.map(
        (result) =>
          result.diff &&
          expandedDiffs[result.relativePath] && (
            <div key={`${result.relativePath}-diff`} className="file-write-diff">
              <h4 className="mono">{result.relativePath}</h4>
              {result.backupPath && (
                <p className="form-hint">Backup: {result.backupPath}</p>
              )}
              <CodeBlock code={result.diff} language="diff" />
            </div>
          )
      )}
    </div>
  );
}
