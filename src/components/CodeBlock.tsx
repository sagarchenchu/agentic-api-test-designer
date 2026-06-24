interface CodeBlockProps {
  code: string;
  language?: string;
  className?: string;
}

export default function CodeBlock({ code, language, className = '' }: CodeBlockProps) {
  return (
    <pre className={`code-block ${className}`}>
      <code className={language ? `language-${language}` : undefined}>{code}</code>
    </pre>
  );
}
