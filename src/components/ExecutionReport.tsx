import type { ExecutionResult } from '../types';
import StatusBadge from './StatusBadge';

interface ExecutionReportProps {
  result: ExecutionResult | null;
  onCreateBugDraft: () => void;
  onRerunFailed: () => void;
  onExportReport: () => void;
}

export default function ExecutionReport({
  result,
  onCreateBugDraft,
  onRerunFailed,
  onExportReport,
}: ExecutionReportProps) {
  if (!result) {
    return (
      <div className="empty-state">
        <p>Run the agent with execution mode to view test results.</p>
      </div>
    );
  }

  const { failedScenario } = result;

  return (
    <div className="execution-report">
      <div className="execution-summary">
        <div className="summary-stat">
          <span className="summary-stat-value">{result.total}</span>
          <span className="summary-stat-label">Total Scenarios</span>
        </div>
        <div className="summary-stat summary-stat--success">
          <span className="summary-stat-value">{result.passed}</span>
          <span className="summary-stat-label">Passed</span>
        </div>
        <div className="summary-stat summary-stat--error">
          <span className="summary-stat-value">{result.failed}</span>
          <span className="summary-stat-label">Failed</span>
        </div>
        <div className="summary-stat">
          <span className="summary-stat-value">{result.skipped}</span>
          <span className="summary-stat-label">Skipped</span>
        </div>
        <div className="summary-stat">
          <span className="summary-stat-value">{result.duration}</span>
          <span className="summary-stat-label">Duration</span>
        </div>
      </div>

      <div className="report-path">
        <strong>Report path:</strong>{' '}
        <code className="mono">{result.reportPath}</code>
      </div>

      {result.failed > 0 && (
        <div className="failed-scenario-card">
          <div className="failed-scenario-header">
            <h3>Failed Scenario</h3>
            <StatusBadge label="Failed" variant="error" />
          </div>
          <dl className="failed-scenario-details">
            <div>
              <dt>Scenario</dt>
              <dd>{failedScenario.scenario}</dd>
            </div>
            <div>
              <dt>Expected</dt>
              <dd>{failedScenario.expected}</dd>
            </div>
            <div>
              <dt>Actual</dt>
              <dd className="text-error">{failedScenario.actual}</dd>
            </div>
            <div>
              <dt>Likely Root Cause</dt>
              <dd>{failedScenario.rootCause}</dd>
            </div>
          </dl>
          <div className="evidence-section">
            <h4>Evidence</h4>
            <ul>
              <li>Endpoint: {failedScenario.endpoint}</li>
              <li>Correlation ID: {failedScenario.correlationId}</li>
              <li>
                Response Body:{' '}
                <code className="mono">{failedScenario.responseBody}</code>
              </li>
            </ul>
          </div>
        </div>
      )}

      <div className="action-bar">
        <button type="button" className="btn btn-secondary btn-sm" onClick={onCreateBugDraft}>
          Create Bug Draft
        </button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRerunFailed}>
          Re-run Failed Tests
        </button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onExportReport}>
          Export Report
        </button>
      </div>
    </div>
  );
}
