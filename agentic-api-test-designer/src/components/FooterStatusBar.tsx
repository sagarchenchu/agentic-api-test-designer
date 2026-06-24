interface FooterStatusBarProps {
  message: string;
  isRunning: boolean;
}

export default function FooterStatusBar({ message, isRunning }: FooterStatusBarProps) {
  return (
    <footer className="footer-status-bar">
      <div className="footer-status-left">
        {isRunning && <span className="status-spinner" aria-hidden="true" />}
        <span>{message}</span>
      </div>
      <div className="footer-status-right">
        <span>Agentic API Test Designer v0.1.0</span>
        <span className="footer-divider">|</span>
        <span>UI Prototype</span>
      </div>
    </footer>
  );
}
