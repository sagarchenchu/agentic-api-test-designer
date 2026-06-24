import type { Environment } from '../types';
import StatusBadge from './StatusBadge';

interface HeaderProps {
  environment: Environment;
  onEnvironmentChange: (env: Environment) => void;
  theme: 'light' | 'dark';
  onThemeToggle: () => void;
}

export default function Header({
  environment,
  onEnvironmentChange,
  theme,
  onThemeToggle,
}: HeaderProps) {
  return (
    <header className="app-header">
      <div className="header-left">
        <div className="header-brand">
          <h1 className="app-title">Agentic API Test Designer</h1>
          <p className="app-subtitle">
            Generate API test cases and BDD automation from Jira stories and
            Swagger/OpenAPI specs.
          </p>
        </div>
      </div>
      <div className="header-right">
        <StatusBadge label="UI Prototype" variant="neutral" />
        <label className="env-select-label">
          <span className="sr-only">Environment</span>
          <select
            className="env-select"
            value={environment}
            onChange={(e) => onEnvironmentChange(e.target.value as Environment)}
          >
            <option value="QA">QA</option>
            <option value="UAT">UAT</option>
            <option value="DEV">DEV</option>
          </select>
        </label>
        <button
          type="button"
          className="btn btn-icon"
          onClick={onThemeToggle}
          title={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`}
          aria-label="Toggle theme"
        >
          {theme === 'light' ? '🌙' : '☀️'}
        </button>
      </div>
    </header>
  );
}
