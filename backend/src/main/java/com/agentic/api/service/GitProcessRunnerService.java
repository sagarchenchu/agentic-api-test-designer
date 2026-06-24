package com.agentic.api.service;

import com.agentic.api.service.ProcessRunnerService.ProcessRunResult;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class GitProcessRunnerService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    private final ProcessRunnerService processRunnerService;

    public GitProcessRunnerService(ProcessRunnerService processRunnerService) {
        this.processRunnerService = processRunnerService;
    }

    public GitCommandResult run(List<String> command, Path workingDirectory) {
        return run(command, workingDirectory, DEFAULT_TIMEOUT_SECONDS);
    }

    public GitCommandResult run(List<String> command, Path workingDirectory, int timeoutSeconds) {
        ProcessRunResult result = processRunnerService.run(command, workingDirectory, timeoutSeconds);
        GitCommandResult gitResult = new GitCommandResult();
        gitResult.setExitCode(result.getExitCode());
        gitResult.setTimedOut(result.isTimedOut());
        gitResult.setException(result.getException());
        gitResult.setOutputLines(result.getOutputLines());
        gitResult.setSuccess(result.getExitCode() == 0 && !result.isTimedOut() && result.getException() == null);
        return gitResult;
    }

    public static class GitCommandResult {
        private int exitCode;
        private boolean timedOut;
        private String exception;
        private List<String> outputLines;
        private boolean success;

        public int getExitCode() {
            return exitCode;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public void setTimedOut(boolean timedOut) {
            this.timedOut = timedOut;
        }

        public String getException() {
            return exception;
        }

        public void setException(String exception) {
            this.exception = exception;
        }

        public List<String> getOutputLines() {
            return outputLines;
        }

        public void setOutputLines(List<String> outputLines) {
            this.outputLines = outputLines;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String firstLine() {
            if (outputLines == null || outputLines.isEmpty()) {
                return "";
            }
            return outputLines.get(0).trim();
        }

        public String output() {
            if (outputLines == null || outputLines.isEmpty()) {
                return "";
            }
            return String.join("\n", outputLines);
        }
    }
}
