import { useState } from 'react';
import type { AgentFormValues, FormErrors, JiraConfigStatus, JiraStoryDetails } from '../types';
import KeyValueHeadersEditor from './KeyValueHeadersEditor';

interface AgentInputPanelProps {
  values: AgentFormValues;
  errors: FormErrors;
  isRunning: boolean;
  jiraConfigStatus: JiraConfigStatus | null;
  jiraStoryDetails: JiraStoryDetails | null;
  onChange: (values: AgentFormValues) => void;
  onRunAgent: () => void;
  onGenerateMatrix: () => void;
  onExtractContract: () => void;
  onGenerateAutomationPackage: () => void;
  onFetchJiraStory: () => void;
  onPostJiraSummary: () => void;
  onLinkPrToJira: () => void;
  onClear: () => void;
}

export default function AgentInputPanel({
  values,
  errors,
  isRunning,
  jiraConfigStatus,
  jiraStoryDetails,
  onChange,
  onRunAgent,
  onGenerateMatrix,
  onExtractContract,
  onGenerateAutomationPackage,
  onFetchJiraStory,
  onPostJiraSummary,
  onLinkPrToJira,
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
          <div className="form-actions form-actions--inline">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onFetchJiraStory}
              disabled={isRunning || !values.jiraStoryKey.trim()}
            >
              Fetch Jira Story
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onPostJiraSummary}
              disabled={isRunning || !values.jiraStoryKey.trim()}
            >
              Post Jira Summary
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onLinkPrToJira}
              disabled={isRunning || !values.jiraStoryKey.trim()}
            >
              Link PR to Jira
            </button>
          </div>
          {jiraConfigStatus && (
            <span className="form-hint">
              Jira: {jiraConfigStatus.enabled && jiraConfigStatus.configured
                ? `connected (${jiraConfigStatus.baseUrl})`
                : jiraConfigStatus.message ?? 'not configured'}
            </span>
          )}
          {jiraStoryDetails && (
            <div className="card-inline jira-story-meta">
              <p>
                <strong>{jiraStoryDetails.summary}</strong>
              </p>
              <p className="mono">
                {jiraStoryDetails.status} · {jiraStoryDetails.issueType} · {jiraStoryDetails.priority}
              </p>
              {jiraStoryDetails.url && (
                <p>
                  <a href={jiraStoryDetails.url} target="_blank" rel="noreferrer">
                    {jiraStoryDetails.url}
                  </a>
                </p>
              )}
              {jiraStoryDetails.labels.length > 0 && (
                <p>Labels: {jiraStoryDetails.labels.join(', ')}</p>
              )}
            </div>
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

        <div className="form-group checkbox-group">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={values.overwriteExisting}
              onChange={(e) => update('overwriteExisting', e.target.checked)}
            />
            Overwrite existing files
          </label>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={values.createBackup}
              onChange={(e) => update('createBackup', e.target.checked)}
            />
            Create backup before overwrite
          </label>
        </div>

        <div className="form-row">
          <div className="form-group form-group--flex">
            <label htmlFor="testTag">Test Tag</label>
            <input
              id="testTag"
              type="text"
              className="form-input"
              placeholder="@PAY-1234"
              value={values.testTag}
              onChange={(e) => update('testTag', e.target.value)}
            />
          </div>
          <div className="form-group form-group--narrow">
            <label htmlFor="mavenProfile">Maven Profile</label>
            <input
              id="mavenProfile"
              type="text"
              className="form-input"
              placeholder="qa"
              value={values.mavenProfile}
              onChange={(e) => update('mavenProfile', e.target.value)}
            />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="timeoutSeconds">Timeout Seconds</label>
          <input
            id="timeoutSeconds"
            type="number"
            min={30}
            max={900}
            className="form-input"
            value={values.timeoutSeconds}
            onChange={(e) => update('timeoutSeconds', Number(e.target.value) || 300)}
          />
        </div>

        <div className="form-row">
          <div className="form-group form-group--flex">
            <label htmlFor="baseBranch">Base Branch</label>
            <input
              id="baseBranch"
              type="text"
              className="form-input"
              placeholder="main"
              value={values.baseBranch}
              onChange={(e) => update('baseBranch', e.target.value)}
            />
          </div>
          <div className="form-group form-group--flex">
            <label htmlFor="remoteName">Remote Name</label>
            <input
              id="remoteName"
              type="text"
              className="form-input"
              placeholder="origin"
              value={values.remoteName}
              onChange={(e) => update('remoteName', e.target.value)}
            />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="newBranchName">New Branch Name</label>
          <input
            id="newBranchName"
            type="text"
            className="form-input"
            placeholder="feature/PAY-1234-api-tests"
            value={values.newBranchName}
            onChange={(e) => update('newBranchName', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="commitMessage">Commit Message</label>
          <input
            id="commitMessage"
            type="text"
            className="form-input"
            placeholder="Add API automation tests for PAY-1234"
            value={values.commitMessage}
            onChange={(e) => update('commitMessage', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label htmlFor="prTitle">PR Title</label>
          <input
            id="prTitle"
            type="text"
            className="form-input"
            placeholder="PAY-1234 Add API automation tests"
            value={values.prTitle}
            onChange={(e) => update('prTitle', e.target.value)}
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
          <label htmlFor="testGenerationMode">Test Generation Mode</label>
          <select
            id="testGenerationMode"
            className="form-select"
            value={values.testGenerationMode}
            onChange={(e) =>
              update(
                'testGenerationMode',
                e.target.value as AgentFormValues['testGenerationMode']
              )
            }
          >
            <option value="deterministic">Deterministic Swagger Rules</option>
            <option value="ai-assisted">AI-assisted</option>
          </select>
        </div>

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
            onClick={onExtractContract}
            disabled={isRunning}
          >
            Extract API Contract
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
            onClick={onGenerateAutomationPackage}
            disabled={isRunning}
          >
            Generate Automation Package
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
