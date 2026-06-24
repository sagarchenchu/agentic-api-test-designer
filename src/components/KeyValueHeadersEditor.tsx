import type { HeaderEntry } from '../types';

interface KeyValueHeadersEditorProps {
  headers: HeaderEntry[];
  onChange: (headers: HeaderEntry[]) => void;
}

export default function KeyValueHeadersEditor({
  headers,
  onChange,
}: KeyValueHeadersEditorProps) {
  const updateHeader = (id: string, field: 'key' | 'value', value: string) => {
    onChange(
      headers.map((h) => (h.id === id ? { ...h, [field]: value } : h))
    );
  };

  const addRow = () => {
    onChange([
      ...headers,
      { id: crypto.randomUUID(), key: '', value: '' },
    ]);
  };

  const removeRow = (id: string) => {
    if (headers.length <= 1) return;
    onChange(headers.filter((h) => h.id !== id));
  };

  return (
    <div className="kv-editor">
      <div className="kv-editor-header">
        <span>Key</span>
        <span>Value</span>
        <span className="sr-only">Actions</span>
      </div>
      {headers.map((header) => (
        <div key={header.id} className="kv-editor-row">
          <input
            type="text"
            className="form-input"
            value={header.key}
            onChange={(e) => updateHeader(header.id, 'key', e.target.value)}
            placeholder="Header name"
          />
          <input
            type="text"
            className="form-input"
            value={header.value}
            onChange={(e) => updateHeader(header.id, 'value', e.target.value)}
            placeholder="Header value"
          />
          <button
            type="button"
            className="btn btn-icon btn-ghost"
            onClick={() => removeRow(header.id)}
            title="Remove header"
            aria-label="Remove header"
          >
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="btn btn-ghost btn-sm" onClick={addRow}>
        + Add Header
      </button>
    </div>
  );
}
