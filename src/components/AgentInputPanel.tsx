import { useState } from 'react';
import type { AgentFormValues, FormErrors } from '../types';
import KeyValueHeadersEditor from './KeyValueHeadersEditor';

interface AgentInputPanelProps {
  values: AgentFormValues;
  errors: FormErrors;
  isRunning: boolean;
  onChange: (values: AgentFormValues) => void;
  onRunAgent: () => void;
  onGenerateMatrix: () => void;
  onClear: () => void;
}

export default function AgentInputPanel({
  values,
  errors,
  isRunning,
  onChange,
  onRunAgent,
  onGenerateMatrix,
  onClear,
}: AgentInputPanelProps) {
  const [swaggerJsonOpen, setSwaggerJsonOpen] = useState(false);

  const update = <K extends keyof AgentFormValues>(
    field: K,
    value: AgentFormValues[K]
  ) => {
    onChange({ ...values, [field]: value });
  };

  return (
    <aside className="agent-input-panel card">
      <h2 className="panel-title">Agent Input</h2>

      <form
        className="agent-form"
        onSubmit={(e) => {
          e.preventDefault();
          onRunAgent();
        }}
      >
        <div className="form-group">
          <label htmlFor="jiraStoryKey">
            Jira Story Key <span className="required">*</span>
          </label>
          <input
            id="jiraStoryKey"
            type="text"
            className={`form-input ${errors.jiraStoryKey ? 'form-input--error' : ''}`}
            placeholder="PAY-1234"
            value={values.jiraStoryKey}
            onChange={(e) => update('jiraStoryKey', e.target.value)}
          />
          {errors.jiraStoryKey && (
            <span className="form-error">{errors.jiraStoryKey}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="jiraStoryText">Jira Story Text</label>
          <textarea
            id="jiraStoryText"
            className="form-textarea form-textarea--large"
            placeholder="Paste Jira story description, acceptance criteria, endpoint details, headers, request/response expectations..."
            rows={6}
            value={values.jiraStoryText}
            onChange={(e) => update('jiraStoryText', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="swaggerUrl">Swagger/OpenAPI URL</label>
          <input
            id="swaggerUrl"
            type="url"
            className={`form-input ${errors.swagger ? 'form-input--error' : ''}`}
            placeholder="https://qa-api.company.com/swagger/v1/swagger.json"
            value={values.swaggerUrl}
            onChange={(e) => update('swaggerUrl', e.target.value)}
          />
        </div>

        <div className="form-group">
          <button
            type="button"
            className="collapsible-trigger"
            onClick={() => setSwaggerJsonOpen(!swaggerJsonOpen)}
            aria-expanded={swaggerJsonOpen}
          >
            <span>Swagger/OpenAPI JSON</span>
            <span className="collapsible-icon">{swaggerJsonOpen ? '▼' : '▶'}</span>
          </button>
          {swaggerJsonOpen && (
            <textarea
              id="swaggerJson"
              className={`form-textarea ${errors.swagger ? 'form-input--error' : ''}`}
              placeholder="Optional: paste Swagger/OpenAPI JSON directly"
              rows={5}
              value={values.swaggerJson}
              onChange={(e) => update('swaggerJson', e.target.value)}
            />
          )}
          {errors.swagger && (
            <span className="form-error">{errors.swagger}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="baseApiUrl">
            Base API URL <span className="required">*</span>
          </label>
          <input
            id="baseApiUrl"
            type="url"
            className={`form-input ${errors.baseApiUrl ? 'form-input--error' : ''}`}
            placeholder="https://qa-api.company.com"
            value={values.baseApiUrl}
            onChange={(e) => update('baseApiUrl', e.target.value)}
          />
          {errors.baseApiUrl && (
            <span className="form-error">{errors.baseApiUrl}</span>
          )}
        </div>

        <div className="form-row">
          <div className="form-group form-group--flex">
            <label htmlFor="endpointPath">
              Endpoint Path <span className="required">*</span>
            </label>
            <input
              id="endpointPath"
              type="text"
              className={`form-input ${errors.endpointPath ? 'form-input--error' : ''}`}
              placeholder="/api/payments"
              value={values.endpointPath}
              onChange={(e) => update('endpointPath', e.target.value)}
            />
            {errors.endpointPath && (
              <span className="form-error">{errors.endpointPath}</span>
            )}
          </div>

          <div className="form-group form-group--narrow">
            <label htmlFor="httpMethod">
              HTTP Method <span className="required">*</span>
            </label>
            <select
              id="httpMethod"
              className="form-select"
              value={values.httpMethod}
              onChange={(e) =>
                update('httpMethod', e.target.value as AgentFormValues['httpMethod'])
              }
            >
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="PATCH">PATCH</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
        </div>

        <div className="form-group">
          <label>Headers</label>
          <KeyValueHeadersEditor
            headers={values.headers}
            onChange={(headers) => update('headers', headers)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="credentialRef">Credential Reference</label>
          <input
            id="credentialRef"
            type="text"
            className={`form-input ${errors.credentialRef ? 'form-input--error' : ''}`}
            placeholder="qa_api_user"
            value={values.credentialRef}
            onChange={(e) => update('credentialRef', e.target.value)}
          />
          <span className="form-hint">
            Do not enter raw passwords. Use credential reference only.
          </span>
          {errors.credentialRef && (
            <span className="form-error">{errors.credentialRef}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="projectPath">Local Project Path</label>
          <input
            id="projectPath"
            type="text"
            className="form-input"
            placeholder="C:\repos\api-automation-framework"
            value={values.projectPath}
            onChange={(e) => update('projectPath', e.target.value)}
          />
        </div>

        <fieldset className="form-group">
          <legend>Execution Mode</legend>
          <div className="radio-group">
            <label className="radio-label">
              <input
                type="radio"
                name="executionMode"
                value="generate-test-cases"
                checked={values.executionMode === 'generate-test-cases'}
                onChange={() => update('executionMode', 'generate-test-cases')}
              />
              Generate test cases only
            </label>
            <label className="radio-label">
              <input
                type="radio"
                name="executionMode"
                value="generate-automation"
                checked={values.executionMode === 'generate-automation'}
                onChange={() => update('executionMode', 'generate-automation')}
              />
              Generate automation only
            </label>
            <label className="radio-label">
              <input
                type="radio"
                name="executionMode"
                value="generate-execute"
                checked={values.executionMode === 'generate-execute'}
                onChange={() => update('executionMode', 'generate-execute')}
              />
              Generate + execute
            </label>
            <label className="radio-label">
              <input
                type="radio"
                name="executionMode"
                value="generate-execute-pr"
                checked={values.executionMode === 'generate-execute-pr'}
                onChange={() => update('executionMode', 'generate-execute-pr')}
              />
              Generate + execute + create PR
            </label>
          </div>
        </fieldset>

        <div className="form-group">
          <label htmlFor="frameworkType">Framework Type</label>
          <select
            id="frameworkType"
            className="form-select"
            value={values.frameworkType}
            onChange={(e) =>
              update('frameworkType', e.target.value as AgentFormValues['frameworkType'])
            }
          >
            <option value="restassured-cucumber-serenity">
              RestAssured + Cucumber + Serenity
            </option>
            <option value="restassured-only">RestAssured only</option>
            <option value="playwright-api">Playwright API</option>
            <option value="postman-newman">Postman/Newman</option>
            <option value="custom">Custom</option>
          </select>
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isRunning}
          >
            {isRunning ? 'Running...' : 'Run Agent'}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onGenerateMatrix}
            disabled={isRunning}
          >
            Generate Test Matrix
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClear}
            disabled={isRunning}
          >
            Clear
          </button>
          <button type="button" className="btn btn-disabled" disabled title="Available after execution">
            Create PR
          </button>
        </div>
      </form>
    </aside>
  );
}
