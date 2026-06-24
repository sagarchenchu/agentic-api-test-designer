import type { TestExecutionResponse } from '../types';
import CodeBlock from './CodeBlock';
import StatusBadge from './StatusBadge';

interface TestExecutionViewProps {
  result: TestExecutionResponse | null;
  isRunning: boolean;
  onPreview: () => void;
  onRun: () => void;
}

function statusVariant(
  status: string
): 'success' | 'warning' | 'error' | 'info' | 'neutral' | 'running' {
  switch (status) {
    case 'PASSED':
    case 'READY':
      return 'success';
    case 'RUNNING':
      return 'running';
    case 'FAILED':
    case 'TIMEOUT':
      return 'warning';
    case 'ERROR':
      return 'error';
    default:
      return 'neutral';
  }
}

export default function TestExecutionView({
  result,
  isRunning,
  onPreview,
  onRun,
}: TestExecutionViewProps) {
  return (
    <div className="test-execution-view">
      <div className="action-bar">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onPreview}
          disabled={isRunning}
        >
          Preview Test Execution
        </button>
        <button
          type="button"
          className="btn btn-primary btn-sm"
          onClick={onRun}
          disabled={isRunning}
        >
          {isRunning ? 'Running Maven Tests...' : 'Run Maven Tests'}
        </button>
      </div>

      {!result ? (
        <div className="empty-state">
          <p>
            Set the project path and test tag, then preview or run Maven tests against the
            automation project.
          </p>
        </div>
      ) : (
        <>
          <div className="file-write-summary card-inline">
            <div className="test-execution-header">
              <h3>Execution Result</h3>
              <StatusBadge label={result.status} variant={statusVariant(result.status)} />
            </div>
            <p className="mono">{result.command}</p>
            <div className="summary-grid">
              <span>Execution ID: {result.executionId}</span>
              <span>Exit Code: {result.exitCode ?? '—'}</span>
              <span>Duration: {result.durationMs ? `${result.durationMs} ms` : '—'}</span>
              <span>Total: {result.summary.total}</span>
              <span>Passed: {result.summary.passed}</span>
              <span>Failed: {result.summary.failed}</span>
              <span>Skipped: {result.summary.skipped}</span>
              <span>Errors: {result.summary.errors}</span>
            </div>
            <p className="mono file-write-project-path">{result.projectPath}</p>
          </div>

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

          <div className="card-inline">
            <h4>Report Paths</h4>
            <ul className="report-path-list">
              <li>Surefire: {result.reportPaths.surefire ?? '—'}</li>
              <li>Failsafe: {result.reportPaths.failsafe ?? '—'}</li>
              <li>Serenity: {result.reportPaths.serenity ?? '—'}</li>
              <li>Cucumber JSON: {result.reportPaths.cucumberJson ?? '—'}</li>
            </ul>
          </div>

          {result.failedScenarios.length > 0 && (
            <div className="table-wrapper">
              <h4>Failed Scenarios</h4>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Feature</th>
                    <th>Scenario</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {result.failedScenarios.map((scenario) => (
                    <tr key={`${scenario.feature}-${scenario.scenario}`}>
                      <td>{scenario.feature ?? '—'}</td>
                      <td>{scenario.scenario}</td>
                      <td>{scenario.errorMessage ?? scenario.rootCause ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {result.logTail && (
            <div className="file-write-diff">
              <h4>Log Tail</h4>
              <CodeBlock code={result.logTail} language="shell" />
            </div>
          )}
        </>
      )}
    </div>
  );
}
