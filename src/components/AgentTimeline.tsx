import type { TimelineStep } from '../types';
import { stepStatusVariant } from './StatusBadge';

interface AgentTimelineProps {
  steps: TimelineStep[];
}

const statusLabels: Record<string, string> = {
  pending: 'Pending',
  running: 'Running',
  completed: 'Completed',
  failed: 'Failed',
};

export default function AgentTimeline({ steps }: AgentTimelineProps) {
  return (
    <div className="agent-timeline card">
      <h2 className="timeline-title">Agent Progress</h2>
      <ol className="timeline-list">
        {steps.map((step, index) => (
          <li
            key={step.id}
            className={`timeline-item timeline-item--${step.status}`}
          >
            <div className="timeline-marker">
              <span className="timeline-dot" />
              {index < steps.length - 1 && <span className="timeline-line" />}
            </div>
            <div className="timeline-content">
              <span className="timeline-label">{step.label}</span>
              <span
                className={`timeline-status timeline-status--${stepStatusVariant(step.status)}`}
              >
                {statusLabels[step.status]}
              </span>
            </div>
          </li>
        ))}
      </ol>
    </div>
  );
}
