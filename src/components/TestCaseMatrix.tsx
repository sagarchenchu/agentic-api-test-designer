import type { TestCase } from '../types';
import StatusBadge, { testTypeVariant } from './StatusBadge';

interface TestCaseMatrixProps {
  testCases: TestCase[];
  warnings?: string[];
  assumptions?: string[];
}

export default function TestCaseMatrix({
  testCases,
  warnings = [],
  assumptions = [],
}: TestCaseMatrixProps) {
  if (testCases.length === 0) {
    return (
      <div className="empty-state">
        <p>Run the agent or generate a test matrix to see scenarios.</p>
      </div>
    );
  }

  const showSource = testCases.some((tc) => tc.source);

  return (
    <div className="test-case-matrix">
      {warnings.length > 0 && (
        <div className="contract-warnings matrix-meta">
          <h4>Warnings</h4>
          <ul>
            {warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      {assumptions.length > 0 && (
        <div className="matrix-assumptions matrix-meta">
          <h4>Assumptions</h4>
          <ul>
            {assumptions.map((assumption) => (
              <li key={assumption}>{assumption}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Test Case ID</th>
              <th>Scenario Name</th>
              <th>Type</th>
              <th>Input Variation</th>
              <th>Expected Status</th>
              <th>Expected Validation</th>
              <th>Priority</th>
              <th>Automation Status</th>
              {showSource && <th>Source</th>}
            </tr>
          </thead>
          <tbody>
            {testCases.map((tc) => (
              <tr key={tc.id}>
                <td className="mono">{tc.id}</td>
                <td>{tc.scenarioName}</td>
                <td>
                  <StatusBadge label={tc.type} variant={testTypeVariant(tc.type)} />
                </td>
                <td>{tc.inputVariation}</td>
                <td>{tc.expectedStatus}</td>
                <td>{tc.expectedValidation}</td>
                <td>
                  <StatusBadge
                    label={tc.priority}
                    variant={tc.priority === 'High' ? 'warning' : 'neutral'}
                  />
                </td>
                <td>
                  <StatusBadge label={tc.automationStatus} variant="success" />
                </td>
                {showSource && (
                  <td>
                    {tc.source ? (
                      <StatusBadge label={tc.source} variant="info" />
                    ) : (
                      '—'
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
