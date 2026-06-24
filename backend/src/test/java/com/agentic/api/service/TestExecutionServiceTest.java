package com.agentic.api.service;

import com.agentic.api.model.TestExecutionRequest;
import com.agentic.api.model.TestExecutionResponse;
import com.agentic.api.service.ProcessRunnerService.ProcessRunResult;
import com.agentic.api.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestExecutionServiceTest {

    @Mock
    private ProcessRunnerService processRunnerService;

    private TestExecutionService testExecutionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testExecutionService = new TestExecutionService(
                new MavenCommandBuilder(),
                processRunnerService,
                new TestReportParserService(new com.fasterxml.jackson.databind.ObjectMapper()),
                TestSupport.permissivePathPolicy(),
                TestSupport.operationConfirmationService(),
                TestSupport.mockRunHistoryService(),
                TestSupport.secretMaskingService()
        );
    }

    @Test
    void previewReturnsReadyWithCommand() throws Exception {
        Path project = createMavenProject();

        TestExecutionResponse response = testExecutionService.previewTestExecution(request(project));

        assertEquals("READY", response.getStatus());
        assertTrue(response.getCommand().contains("mvn clean verify"));
        assertTrue(response.getCommand().contains("@PAY-1234"));
    }

    @Test
    void rejectsMissingPomXml() throws Exception {
        Path project = Files.createDirectories(tempDir.resolve("no-pom"));

        TestExecutionResponse response = testExecutionService.previewTestExecution(request(project));

        assertEquals("ERROR", response.getStatus());
        assertTrue(response.getErrors().stream().anyMatch(e -> e.contains("pom.xml")));
    }

    @Test
    void rejectsUnsafeCommandInput() throws Exception {
        Path project = createMavenProject();
        TestExecutionRequest request = request(project);
        request.setTestTag("@PAY-1234; rm");

        TestExecutionResponse response = testExecutionService.previewTestExecution(request);

        assertEquals("ERROR", response.getStatus());
        assertFalse(response.getErrors().isEmpty());
    }

    @Test
    void runMapsTimeoutStatus() throws Exception {
        Path project = createMavenProject();
        ProcessRunResult timedOut = new ProcessRunResult();
        timedOut.setTimedOut(true);
        timedOut.setExitCode(-1);
        timedOut.setOutputLines(List.of("line1", "line2"));
        when(processRunnerService.run(anyList(), any(Path.class), anyInt())).thenReturn(timedOut);

        TestExecutionResponse response = testExecutionService.runTestExecution(request(project));

        assertEquals("TIMEOUT", response.getStatus());
        assertTrue(response.getLogTail().contains("line2"));
    }

    @Test
    void getStoredExecutionById() throws Exception {
        Path project = createMavenProject();
        ProcessRunResult success = new ProcessRunResult();
        success.setExitCode(0);
        success.setOutputLines(List.of("BUILD SUCCESS"));
        when(processRunnerService.run(anyList(), any(Path.class), anyInt())).thenReturn(success);

        TestExecutionResponse run = testExecutionService.runTestExecution(request(project));
        TestExecutionResponse stored = testExecutionService.getTestExecution(run.getExecutionId());

        assertEquals(run.getExecutionId(), stored.getExecutionId());
        assertEquals("PASSED", stored.getStatus());
    }

    private Path createMavenProject() throws Exception {
        Path project = tempDir.resolve("automation-project");
        Files.createDirectories(project);
        Files.writeString(project.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
        return project;
    }

    private TestExecutionRequest request(Path project) {
        TestExecutionRequest request = new TestExecutionRequest();
        request.setProjectPath(project.toString());
        request.setMavenCommand("mvn clean verify");
        request.setTestTag("@PAY-1234");
        request.setProfile("qa");
        request.setEnvironment("QA");
        request.setTimeoutSeconds(300);
        return request;
    }
}
