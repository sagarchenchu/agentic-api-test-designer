import type { StepStatus } from '../types';

interface StatusBadgeProps {
  label: string;
  variant?: 'neutral' | 'success' | 'warning' | 'error' | 'info' | 'running';
}

export default function StatusBadge({
  label,
  variant = 'neutral',
}: StatusBadgeProps) {
  return <span className={`status-badge status-badge--${variant}`}>{label}</span>;
}

export function stepStatusVariant(
  status: StepStatus
): StatusBadgeProps['variant'] {
  switch (status) {
    case 'completed':
      return 'success';
    case 'running':
      return 'running';
    case 'failed':
      return 'error';
    default:
      return 'neutral';
  }
}

export function testTypeVariant(
  type: string
): StatusBadgeProps['variant'] {
  switch (type) {
    case 'Positive':
      return 'success';
    case 'Negative':
      return 'error';
    case 'Boundary':
      return 'warning';
    case 'Security':
      return 'info';
    case 'Schema':
      return 'neutral';
    case 'Business':
      return 'info';
    default:
      return 'neutral';
  }
}
