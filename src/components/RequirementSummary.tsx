import type { RequirementSummary } from '../types';

interface RequirementSummaryProps {
  summary: RequirementSummary | null;
}

export default function RequirementSummaryView({ summary }: RequirementSummaryProps) {
  if (!summary) {
    return (
      <div className="empty-state">
        <p>Run the agent to extract requirement summary from your inputs.</p>
      </div>
    );
  }

  const sections = [
    { label: 'Jira Key', value: summary.jiraKey },
    { label: 'Endpoint', value: summary.endpoint },
    { label: 'Method', value: summary.method },
    {
      label: 'Required Headers',
      value: summary.requiredHeaders,
      isList: true,
    },
    {
      label: 'Request Body Fields',
      value: summary.requestBodyFields,
      isList: true,
    },
    {
      label: 'Expected Status Codes',
      value: summary.expectedStatusCodes,
      isList: true,
    },
    {
      label: 'Business Rules',
      value: summary.businessRules,
      isList: true,
    },
    {
      label: 'Assumptions',
      value: summary.assumptions,
      isList: true,
    },
  ];

  return (
    <div className="requirement-summary">
      {sections.map((section) => (
        <div key={section.label} className="summary-card">
          <h3 className="summary-label">{section.label}</h3>
          {section.isList ? (
            <ul className="summary-list">
              {(section.value as string[]).map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          ) : (
            <p className="summary-value">{section.value as string}</p>
          )}
        </div>
      ))}
    </div>
  );
}
