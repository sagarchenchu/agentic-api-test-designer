import { useState } from 'react';
import CodeBlock from './CodeBlock';

interface GeneratedBddPreviewProps {
  featureContent: string;
  downloadFilename?: string;
  warnings?: string[];
  assumptions?: string[];
  onRegenerate: () => void;
  onGenerateFromMatrix?: () => void;
}

export default function GeneratedBddPreview({
  featureContent,
  downloadFilename = 'feature.feature',
  warnings = [],
  assumptions = [],
  onRegenerate,
  onGenerateFromMatrix,
}: GeneratedBddPreviewProps) {
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'error'>('idle');

  if (!featureContent) {
    return (
      <div className="empty-state">
        <p>Run the agent or generate BDD from the test matrix to see feature files.</p>
        {onGenerateFromMatrix && (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onGenerateFromMatrix}
          >
            Generate BDD from Test Matrix
          </button>
        )}
      </div>
    );
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(featureContent);
      setCopyStatus('copied');
      setTimeout(() => setCopyStatus('idle'), 2000);
    } catch {
      setCopyStatus('error');
      setTimeout(() => setCopyStatus('idle'), 2000);
    }
  };

  const handleDownload = () => {
    const blob = new Blob([featureContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = downloadFilename;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="bdd-preview">
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
      <div className="action-bar">
        {onGenerateFromMatrix && (
          <button type="button" className="btn btn-secondary btn-sm" onClick={onGenerateFromMatrix}>
            Generate BDD from Test Matrix
          </button>
        )}
        <button type="button" className="btn btn-secondary btn-sm" onClick={handleCopy}>
          {copyStatus === 'copied' ? 'Copied!' : copyStatus === 'error' ? 'Copy failed' : 'Copy Feature File'}
        </button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={handleDownload}>
          Download Feature File
        </button>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRegenerate}>
          Regenerate
        </button>
      </div>
      <CodeBlock code={featureContent} language="gherkin" />
    </div>
  );
}
