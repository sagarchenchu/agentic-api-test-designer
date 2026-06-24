package com.agentic.api.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessRunnerService {

    private static final int OUTPUT_READER_JOIN_SECONDS = 2;

    public ProcessRunResult run(List<String> command, Path workingDirectory, int timeoutSeconds) {
        ProcessRunResult result = new ProcessRunResult();
        List<String> outputLines = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);

            Process process = builder.start();

            Future<?> outputReader = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLines.add(line);
                    }
                } catch (Exception ignored) {
                    // Process may be destroyed on timeout while the reader is still open.
                }
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setTimedOut(true);
                result.setExitCode(-1);
            } else {
                result.setExitCode(process.exitValue());
            }

            try {
                outputReader.get(OUTPUT_READER_JOIN_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                outputReader.cancel(true);
            }
        } catch (Exception ex) {
            result.setException(ex.getMessage());
            result.setExitCode(-1);
        } finally {
            executor.shutdownNow();
        }

        result.setOutputLines(new ArrayList<>(outputLines));
        return result;
    }

    public static class ProcessRunResult {
        private int exitCode;
        private boolean timedOut;
        private String exception;
        private List<String> outputLines = new ArrayList<>();

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

        public String fullOutput() {
            return String.join("\n", outputLines);
        }
    }
}
