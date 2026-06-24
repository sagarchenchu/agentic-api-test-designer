import type { GeneratedFile } from '../types';
import CodeBlock from './CodeBlock';

interface GeneratedFilesTreeProps {
  files: GeneratedFile[];
  selectedFile: GeneratedFile | null;
  onSelectFile: (file: GeneratedFile) => void;
  warnings?: string[];
  assumptions?: string[];
  onGenerateFiles?: () => void;
  onPreviewWrite?: () => void;
  onWriteFiles?: () => void;
  canWriteFiles?: boolean;
  isRunning?: boolean;
}

export default function GeneratedFilesTree({
  files,
  selectedFile,
  onSelectFile,
  warnings = [],
  assumptions = [],
  onGenerateFiles,
  onPreviewWrite,
  onWriteFiles,
  canWriteFiles = false,
  isRunning = false,
}: GeneratedFilesTreeProps) {
  if (files.length === 0) {
    return (
      <div className="empty-state">
        <p>Run the agent or generate automation files from the test matrix.</p>
        {onGenerateFiles && (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={onGenerateFiles}
          >
            Generate Automation Files
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="files-tree-layout">
      {(warnings.length > 0 || assumptions.length > 0) && (
        <div className="files-meta">
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
        </div>
      )}
      {(onGenerateFiles || onPreviewWrite || onWriteFiles) && (
        <div className="action-bar">
          {onGenerateFiles && (
            <button type="button" className="btn btn-secondary btn-sm" onClick={onGenerateFiles}>
              Generate Automation Files
            </button>
          )}
          {onPreviewWrite && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onPreviewWrite}
              disabled={isRunning}
            >
              Preview Write to Project
            </button>
          )}
          {onWriteFiles && (
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={onWriteFiles}
              disabled={!canWriteFiles || isRunning}
            >
              Write Files to Project
            </button>
          )}
        </div>
      )}
      <div className="file-tree">
        <h3 className="file-tree-title">Generated Files</h3>
        <ul className="file-tree-list">
          {files.map((file) => (
            <li key={file.path}>
              <button
                type="button"
                className={`file-tree-item ${selectedFile?.path === file.path ? 'file-tree-item--active' : ''}`}
                onClick={() => onSelectFile(file)}
              >
                <span className="file-icon">📄</span>
                <span className="file-path">{file.path}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
      <div className="file-viewer">
        {selectedFile ? (
          <>
            <div className="file-viewer-header">
              <span className="mono">{selectedFile.path}</span>
            </div>
            <CodeBlock code={selectedFile.content} language={selectedFile.language} />
          </>
        ) : (
          <div className="empty-state">
            <p>Select a file to preview its content.</p>
          </div>
        )}
      </div>
    </div>
  );
}
