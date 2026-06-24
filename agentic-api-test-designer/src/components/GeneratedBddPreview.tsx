import { useState } from 'react';
import CodeBlock from './CodeBlock';

interface GeneratedBddPreviewProps {
  featureContent: string;
  onRegenerate: () => void;
}

export default function GeneratedBddPreview({
  featureContent,
  onRegenerate,
}: GeneratedBddPreviewProps) {
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'error'>('idle');

  if (!featureContent) {
    return (
      <div className="empty-state">
        <p>Run the agent to generate BDD feature files.</p>
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
    a.download = 'create_payment.feature';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="bdd-preview">
      <div className="action-bar">
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
