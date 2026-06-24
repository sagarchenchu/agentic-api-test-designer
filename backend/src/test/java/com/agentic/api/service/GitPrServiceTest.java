package com.agentic.api.service;

import com.agentic.api.model.GitPrRequest;
import com.agentic.api.model.GitPrResponse;
import com.agentic.api.service.GitProcessRunnerService.GitCommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitPrServiceTest {

    @Mock
    private GitProcessRunnerService processRunnerService;

    private GitPrService gitPrService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitPrService = new GitPrService(
                new GitValidationService(),
                new GitCommandBuilder(),
                processRunnerService,
                new GitStatusParserService()
        );
    }

    @Test
    void previewDoesNotRunMutatingCommands() throws Exception {
        Path project = initGitProject();
        GitPrRequest request = request(project);

        when(processRunnerService.run(eq(List.of("git", "rev-parse", "--show-toplevel")), any()))
                .thenReturn(success("/repo\n"));
        when(processRunnerService.run(eq(List.of("git", "status", "--short")), any()))
                .thenReturn(success(" M src/test/resources/features/payment/create_payment.feature\n"));

        GitPrResponse response = gitPrService.previewGitPr(request);

        assertEquals("READY", response.getStatus());
        verify(processRunnerService, never()).run(eq(List.of("git", "checkout", "main")), any());
        verify(processRunnerService, never()).run(eq(List.of("git", "commit", "-m", request.getCommitMessage())), any());
    }

    @Test
    void createBlocksUnrelatedChanges() throws Exception {
        Path project = initGitProject();
        GitPrRequest request = request(project);

        when(processRunnerService.run(eq(List.of("git", "rev-parse", "--show-toplevel")), any()))
                .thenReturn(success("/repo\n"));
        when(processRunnerService.run(eq(List.of("git", "status", "--short")), any()))
                .thenReturn(success(" M pom.xml\n M src/test/resources/features/payment/create_payment.feature\n"));

        GitPrResponse response = gitPrService.createGitPr(request);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getErrors().stream().anyMatch(e -> e.contains("unrelated changes")));
        verify(processRunnerService, never()).run(argThat(cmd -> cmd.contains("checkout")), any());
    }

    @Test
    void createFailsWhenGhMissing() throws Exception {
        Path project = initGitProject();
        GitPrRequest request = request(project);

        when(processRunnerService.run(eq(List.of("git", "rev-parse", "--show-toplevel")), any()))
                .thenReturn(success("/repo\n"));
        when(processRunnerService.run(eq(List.of("git", "status", "--short")), any()))
                .thenReturn(success(" M src/test/resources/features/payment/create_payment.feature\n"));
        when(processRunnerService.run(eq(List.of("gh", "--version")), any()))
                .thenReturn(failure());

        GitPrResponse response = gitPrService.createGitPr(request);

        assertEquals("FAILED", response.getStatus());
        assertTrue(response.getErrors().stream().anyMatch(e -> e.contains("GitHub CLI not found")));
    }

    @Test
    void createReturnsPrUrlOnSuccess() throws Exception {
        Path project = initGitProject();
        GitPrRequest request = request(project);

        when(processRunnerService.run(eq(List.of("git", "rev-parse", "--show-toplevel")), any()))
                .thenReturn(success("/repo\n"));
        when(processRunnerService.run(eq(List.of("git", "status", "--short")), any()))
                .thenReturn(success(" M src/test/resources/features/payment/create_payment.feature\n"));
        when(processRunnerService.run(eq(List.of("gh", "--version")), any()))
                .thenReturn(success("gh version 2.0.0\n"));
        when(processRunnerService.run(eq(List.of("git", "checkout", "main")), any()))
                .thenReturn(success(""));
        when(processRunnerService.run(eq(List.of("git", "pull", "origin", "main")), any()))
                .thenReturn(success(""));
        when(processRunnerService.run(eq(List.of("git", "checkout", "-b", request.getNewBranchName())), any()))
                .thenReturn(success(""));
        when(processRunnerService.run(argThat(cmd ->
                cmd != null && cmd.size() >= 2 && "git".equals(cmd.get(0)) && "add".equals(cmd.get(1))
        ), any())).thenReturn(success(""));
        when(processRunnerService.run(eq(List.of("git", "commit", "-m", request.getCommitMessage())), any()))
                .thenReturn(success(""));
        when(processRunnerService.run(eq(List.of("git", "rev-parse", "HEAD")), any()))
                .thenReturn(success("abc123def\n"));
        when(processRunnerService.run(eq(List.of("git", "push", "origin", request.getNewBranchName())), any()))
                .thenReturn(success(""));
        when(processRunnerService.run(argThat(cmd ->
                cmd != null && cmd.size() >= 3 && "gh".equals(cmd.get(0)) && "pr".equals(cmd.get(1))
        ), any())).thenReturn(success("https://github.com/org/repo/pull/99\n"));

        GitPrResponse response = gitPrService.createGitPr(request);

        assertEquals("CREATED", response.getStatus());
        assertEquals("abc123def", response.getCommitSha());
        assertEquals("https://github.com/org/repo/pull/99", response.getPrUrl());
    }

    @Test
    void getGitPrReturnsStoredOperation() throws Exception {
        Path project = initGitProject();
        GitPrRequest request = request(project);

        when(processRunnerService.run(anyList(), any()))
                .thenReturn(success(""));

        GitPrResponse created = gitPrService.previewGitPr(request);
        GitPrResponse fetched = gitPrService.getGitPr(created.getOperationId());

        assertEquals(created.getOperationId(), fetched.getOperationId());
    }

    private Path initGitProject() throws Exception {
        Path feature = tempDir.resolve("src/test/resources/features/payment/create_payment.feature");
        Files.createDirectories(feature.getParent());
        Files.writeString(feature, "Feature: payment");
        return tempDir;
    }

    private GitPrRequest request(Path project) {
        GitPrRequest request = new GitPrRequest();
        request.setProjectPath(project.toString());
        request.setJiraStoryKey("PAY-1234");
        request.setNewBranchName("feature/PAY-1234-api-tests");
        request.setCommitMessage("Add API automation tests for PAY-1234");
        request.setPrTitle("PAY-1234 Add API automation tests");
        request.setPrBody("Generated API tests.");
        request.setFilesToCommit(List.of("src/test/resources/features/payment/create_payment.feature"));
        return request;
    }

    private GitCommandResult success(String output) {
        GitCommandResult result = new GitCommandResult();
        result.setExitCode(0);
        result.setSuccess(true);
        result.setOutputLines(List.of(output.split("\n")));
        return result;
    }

    private GitCommandResult failure() {
        GitCommandResult result = new GitCommandResult();
        result.setExitCode(1);
        result.setSuccess(false);
        result.setOutputLines(List.of("command not found"));
        result.setException("Process exited with code 1");
        return result;
    }
}
