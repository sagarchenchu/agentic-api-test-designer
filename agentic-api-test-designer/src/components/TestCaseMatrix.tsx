import type { TestCase } from '../types';
import StatusBadge, { testTypeVariant } from './StatusBadge';

interface TestCaseMatrixProps {
  testCases: TestCase[];
}

export default function TestCaseMatrix({ testCases }: TestCaseMatrixProps) {
  if (testCases.length === 0) {
    return (
      <div className="empty-state">
        <p>Run the agent to generate the test case matrix.</p>
      </div>
    );
  }

  return (
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
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
