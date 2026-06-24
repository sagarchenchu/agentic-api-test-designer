package com.agentic.api.service;

import com.agentic.api.model.*;
import com.agentic.api.service.MavenCommandBuilder.MavenCommandBuildResult;
import com.agentic.api.service.ProcessRunnerService.ProcessRunResult;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TestExecutionService {

    private static final int LOG_TAIL_LINES = 200;
    private static final DateTimeFormatter EXECUTION_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC);

    private final Map<String, TestExecutionResponse> executions = new ConcurrentHashMap<>();
    private final MavenCommandBuilder mavenCommandBuilder;
    private final ProcessRunnerService processRunnerService;
    private final TestReportParserService testReportParserService;
    private final ProjectPathPolicyService projectPathPolicyService;
    private final OperationConfirmationService operationConfirmationService;
    private final RunHistoryService runHistoryService;
    private final SecretMaskingService secretMaskingService;

    public TestExecutionService(
            MavenCommandBuilder mavenCommandBuilder,
            ProcessRunnerService processRunnerService,
            TestReportParserService testReportParserService,
            ProjectPathPolicyService projectPathPolicyService,
            OperationConfirmationService operationConfirmationService,
            RunHistoryService runHistoryService,
            SecretMaskingService secretMaskingService
    ) {
        this.mavenCommandBuilder = mavenCommandBuilder;
        this.processRunnerService = processRunnerService;
        this.testReportParserService = testReportParserService;
        this.projectPathPolicyService = projectPathPolicyService;
        this.operationConfirmationService = operationConfirmationService;
        this.runHistoryService = runHistoryService;
        this.secretMaskingService = secretMaskingService;
    }

    public TestExecutionResponse previewTestExecution(TestExecutionRequest request) {
        TestExecutionResponse response = buildBaseResponse(request);
        response.setStatus("READY");

        if (!validateProjectPath(request, response)) {
            response.setStatus("ERROR");
            return response;
        }

        MavenCommandBuildResult command = mavenCommandBuilder.build(request);
        if (!command.isValid()) {
            response.getErrors().addAll(command.getErrors());
            response.setStatus("ERROR");
            return response;
        }

        Path projectRoot = Paths.get(request.getProjectPath().trim()).toAbsolutePath().normalize();
        response.setCommand(command.getCommandString());
        response.setReportPaths(testReportParserService.detectReportPaths(projectRoot));
        addReportWarnings(response);
        return response;
    }

    public TestExecutionResponse runTestExecution(TestExecutionRequest request) {
        operationConfirmationService.requireConfirmation(request.getConfirmation());
        TestExecutionResponse response = buildBaseResponse(request);
        String startedAt = Instant.now().toString();
        response.setStartedAt(startedAt);

        if (!validateProjectPath(request, response)) {
            response.setStatus("ERROR");
            response.setCompletedAt(Instant.now().toString());
            store(response);
            return response;
        }

        MavenCommandBuildResult command = mavenCommandBuilder.build(request);
        if (!command.isValid()) {
            response.getErrors().addAll(command.getErrors());
            response.setStatus("ERROR");
            response.setCompletedAt(Instant.now().toString());
            store(response);
            return response;
        }

        if (request.isDryRun()) {
            response.setStatus("READY");
            response.setCommand(command.getCommandString());
            response.getWarnings().add("dryRun=true; Maven process was not started");
            response.setCompletedAt(Instant.now().toString());
            store(response);
            return response;
        }

        Path projectRoot = Paths.get(request.getProjectPath().trim()).toAbsolutePath().normalize();
        response.setCommand(command.getCommandString());
        response.setStatus("RUNNING");
        store(response);

        long startMs = System.currentTimeMillis();
        ProcessRunResult processResult = processRunnerService.run(
                command.getArguments(),
                projectRoot,
                request.getTimeoutSeconds()
        );
        long durationMs = System.currentTimeMillis() - startMs;

        response.setDurationMs(durationMs);
        response.setCompletedAt(Instant.now().toString());
        response.setExitCode(processResult.getExitCode());
        response.setLogTail(tail(processResult.getOutputLines()));
        response.setReportPaths(testReportParserService.detectReportPaths(projectRoot));
        response.setSummary(testReportParserService.parseSurefireFailsafe(projectRoot));
        response.setFailedScenarios(testReportParserService.parseCucumberFailures(projectRoot));
        addReportWarnings(response);

        if (processResult.getException() != null) {
            response.getErrors().add(processResult.getException());
            response.setStatus("ERROR");
        } else if (processResult.isTimedOut()) {
            response.getErrors().add("Maven execution timed out after " + request.getTimeoutSeconds() + " seconds");
            response.setStatus("TIMEOUT");
        } else {
            response.setStatus(mapExecutionStatus(processResult.getExitCode(), response.getSummary()));
        }

        store(response);
        maskResponse(response);
        runHistoryService.recordTestExecution(response, null);
        return response;
    }

    public TestExecutionResponse getTestExecution(String executionId) {
        TestExecutionResponse response = executions.get(executionId);
        if (response == null) {
            throw new NoSuchElementException("Test execution not found: " + executionId);
        }
        return response;
    }

    private TestExecutionResponse buildBaseResponse(TestExecutionRequest request) {
        TestExecutionResponse response = new TestExecutionResponse();
        response.setExecutionId("run-" + EXECUTION_ID_FORMAT.format(Instant.now()));
        response.setProjectPath(request.getProjectPath());
        return response;
    }

    private boolean validateProjectPath(TestExecutionRequest request, TestExecutionResponse response) {
        String projectPath = request.getProjectPath();
        if (projectPath == null || projectPath.isBlank()) {
            response.getErrors().add("projectPath is required");
            return false;
        }

        response.getErrors().addAll(secretMaskingService.maskList(
                projectPathPolicyService.validateProjectPath(projectPath)));
        if (!response.getErrors().isEmpty()) {
            return false;
        }

        Path root = Paths.get(projectPath.trim()).toAbsolutePath().normalize();
        if (root.getParent() == null || root.equals(root.getRoot())) {
            response.getErrors().add("projectPath must not be a filesystem root");
            return false;
        }
        if (!Files.exists(root)) {
            response.getErrors().add("projectPath does not exist: " + root);
            return false;
        }
        if (!Files.isDirectory(root)) {
            response.getErrors().add("projectPath is not a directory: " + root);
            return false;
        }
        if (!Files.exists(root.resolve("pom.xml"))) {
            response.getErrors().add("projectPath must contain pom.xml");
            return false;
        }
        return true;
    }

    private String mapExecutionStatus(int exitCode, TestExecutionSummary summary) {
        if (exitCode != 0) {
            return "FAILED";
        }
        if (summary.getFailed() > 0 || summary.getErrors() > 0) {
            return "FAILED";
        }
        return "PASSED";
    }

    private void addReportWarnings(TestExecutionResponse response) {
        TestReportPaths paths = response.getReportPaths();
        List<String> warnings = new ArrayList<>();
        if (paths.getSurefire() == null && paths.getFailsafe() == null) {
            warnings.add("No Surefire/Failsafe report directories detected yet");
        }
        if (paths.getSerenity() == null) {
            warnings.add("Serenity report not found at target/site/serenity/index.html");
        }
        if (paths.getCucumberJson() == null) {
            warnings.add("Cucumber JSON report not found");
        }
        response.getWarnings().addAll(warnings);
    }

    private String tail(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        int from = Math.max(0, lines.size() - LOG_TAIL_LINES);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    private void store(TestExecutionResponse response) {
        executions.put(response.getExecutionId(), response);
    }

    private void maskResponse(TestExecutionResponse response) {
        response.setCommand(secretMaskingService.mask(response.getCommand()));
        response.setLogTail(secretMaskingService.mask(response.getLogTail()));
        response.setWarnings(secretMaskingService.maskList(response.getWarnings()));
        response.setErrors(secretMaskingService.maskList(response.getErrors()));
    }
}
