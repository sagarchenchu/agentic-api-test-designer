package com.agentic.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProcessRunnerServiceTest {

    private ProcessRunnerService processRunnerService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        processRunnerService = new ProcessRunnerService();
    }

    @Test
    void runsFastCommandSuccessfully() {
        ProcessRunnerService.ProcessRunResult result = processRunnerService.run(
                echoCommand("hello"),
                tempDir,
                5
        );

        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutputLines().stream().anyMatch(line -> line.contains("hello")));
    }

    @Test
    void enforcesTimeoutForLongRunningProcess() {
        long startMs = System.currentTimeMillis();

        ProcessRunnerService.ProcessRunResult result = processRunnerService.run(
                longRunningCommand(),
                tempDir,
                1
        );

        long elapsedMs = System.currentTimeMillis() - startMs;

        assertTrue(result.isTimedOut(), "Expected timedOut=true");
        assertEquals(-1, result.getExitCode());
        assertTrue(elapsedMs < TimeUnit.SECONDS.toMillis(5),
                "Timeout should return within a few seconds, but took " + elapsedMs + "ms");
    }

    private List<String> echoCommand(String message) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return List.of("cmd", "/c", "echo", message);
        }
        return List.of("echo", message);
    }

    private List<String> longRunningCommand() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return List.of("ping", "127.0.0.1", "-n", "10");
        }
        return List.of("sleep", "10");
    }
}
