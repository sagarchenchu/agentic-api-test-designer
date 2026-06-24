import type { ReactNode } from 'react';
import type { ApiContract } from '../types';
import StatusBadge from './StatusBadge';

interface ApiContractViewProps {
  contract: ApiContract | null;
  error?: string | null;
}

export default function ApiContractView({ contract, error }: ApiContractViewProps) {
  if (error) {
    return (
      <div className="empty-state empty-state--error">
        <p>{error}</p>
      </div>
    );
  }

  if (!contract) {
    return (
      <div className="empty-state">
        <p>Click &quot;Extract API Contract&quot; to parse Swagger/OpenAPI for this endpoint.</p>
      </div>
    );
  }

  return (
    <div className="api-contract-view">
      <div className="contract-header">
        <div>
          <h3 className="contract-title">
            {contract.summary ?? contract.operationId ?? 'API Operation'}
          </h3>
          <p className="contract-meta mono">
            {contract.httpMethod} {contract.endpointPath}
          </p>
        </div>
        <div className="contract-tags">
          {contract.tags.map((tag) => (
            <StatusBadge key={tag} label={tag} variant="info" />
          ))}
        </div>
      </div>

      {contract.description && (
        <p className="contract-description">{contract.description}</p>
      )}

      {contract.warnings.length > 0 && (
        <div className="contract-warnings">
          <h4>Warnings</h4>
          <ul>
            {contract.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="contract-grid">
        <ContractSection title="Operation">
          <dl className="contract-dl">
            <div>
              <dt>Operation ID</dt>
              <dd className="mono">{contract.operationId ?? '—'}</dd>
            </div>
            <div>
              <dt>Method</dt>
              <dd>{contract.httpMethod}</dd>
            </div>
            <div>
              <dt>Path</dt>
              <dd className="mono">{contract.endpointPath}</dd>
            </div>
          </dl>
        </ContractSection>

        <ContractSection title="Required Headers">
          {contract.requiredHeaders.length === 0 ? (
            <p className="contract-empty">No header parameters defined.</p>
          ) : (
            <ParameterTable parameters={contract.requiredHeaders} />
          )}
        </ContractSection>

        <ContractSection title="Path Parameters">
          {contract.pathParams.length === 0 ? (
            <p className="contract-empty">No path parameters.</p>
          ) : (
            <ParameterTable parameters={contract.pathParams} />
          )}
        </ContractSection>

        <ContractSection title="Query Parameters">
          {contract.queryParams.length === 0 ? (
            <p className="contract-empty">No query parameters.</p>
          ) : (
            <ParameterTable parameters={contract.queryParams} />
          )}
        </ContractSection>

        <ContractSection title="Request Body" className="contract-section--wide">
          {!contract.requestBody ? (
            <p className="contract-empty">No application/json request body defined.</p>
          ) : (
            <>
              <p className="contract-subtitle">
                Content-Type: <span className="mono">{contract.requestBody.contentType}</span>
                {contract.requestBody.required && (
                  <StatusBadge label="Required" variant="warning" />
                )}
              </p>
              {contract.requestBody.requiredFields.length > 0 && (
                <div className="contract-required-fields">
                  <strong>Required fields:</strong>{' '}
                  {contract.requestBody.requiredFields.join(', ')}
                </div>
              )}
              <FieldTable fields={contract.requestBody.fields} />
            </>
          )}
        </ContractSection>

        <ContractSection title="Responses" className="contract-section--wide">
          {contract.responses.length === 0 ? (
            <p className="contract-empty">No responses defined.</p>
          ) : (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Description</th>
                    <th>Content Type</th>
                    <th>Fields</th>
                  </tr>
                </thead>
                <tbody>
                  {contract.responses.map((response) => (
                    <tr key={response.statusCode}>
                      <td>{response.statusCode}</td>
                      <td>{response.description ?? '—'}</td>
                      <td className="mono">{response.contentType ?? '—'}</td>
                      <td>{response.fields?.join(', ') ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </ContractSection>
      </div>
    </div>
  );
}

function ContractSection({
  title,
  children,
  className = '',
}: {
  title: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`summary-card contract-section ${className}`}>
      <h4 className="summary-label">{title}</h4>
      {children}
    </section>
  );
}

function ParameterTable({
  parameters,
}: {
  parameters: ApiContract['requiredHeaders'];
}) {
  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Required</th>
            <th>Type</th>
            <th>Example</th>
          </tr>
        </thead>
        <tbody>
          {parameters.map((param) => (
            <tr key={param.name}>
              <td className="mono">{param.name}</td>
              <td>{param.required ? 'Yes' : 'No'}</td>
              <td>{param.type ?? '—'}</td>
              <td className="mono">{formatValue(param.example)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FieldTable({ fields }: { fields: NonNullable<ApiContract['requestBody']>['fields'] }) {
  return (
    <div className="table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Required</th>
            <th>Constraints</th>
            <th>Example</th>
          </tr>
        </thead>
        <tbody>
          {fields.map((field) => (
            <tr key={field.name}>
              <td className="mono">{field.name}</td>
              <td>{field.type ?? '—'}</td>
              <td>{field.required ? 'Yes' : 'No'}</td>
              <td>{formatConstraints(field)}</td>
              <td className="mono">{formatValue(field.example)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatConstraints(field: NonNullable<ApiContract['requestBody']>['fields'][number]) {
  const parts: string[] = [];
  if (field.enumValues?.length) {
    parts.push(`enum: ${field.enumValues.join('|')}`);
  }
  if (field.minimum != null) {
    parts.push(`min: ${field.minimum}`);
  }
  if (field.maximum != null) {
    parts.push(`max: ${field.maximum}`);
  }
  if (field.format) {
    parts.push(`format: ${field.format}`);
  }
  if (field.nullable) {
    parts.push('nullable');
  }
  return parts.length ? parts.join(', ') : '—';
}

function formatValue(value: unknown) {
  if (value == null) return '—';
  return String(value);
}
