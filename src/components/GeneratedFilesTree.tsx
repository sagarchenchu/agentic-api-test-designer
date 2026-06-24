import type { GeneratedFile } from '../types';
import CodeBlock from './CodeBlock';

interface GeneratedFilesTreeProps {
  files: GeneratedFile[];
  selectedFile: GeneratedFile | null;
  onSelectFile: (file: GeneratedFile) => void;
}

export default function GeneratedFilesTree({
  files,
  selectedFile,
  onSelectFile,
}: GeneratedFilesTreeProps) {
  if (files.length === 0) {
    return (
      <div className="empty-state">
        <p>Run the agent to generate automation files.</p>
      </div>
    );
  }

  return (
    <div className="files-tree-layout">
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
