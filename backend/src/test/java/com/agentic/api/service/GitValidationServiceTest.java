package com.agentic.api.service;

import com.agentic.api.model.GitPrRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitValidationServiceTest {

    private GitValidationService validationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validationService = new GitValidationService();
    }

    @Test
    void acceptsValidBranchNames() throws Exception {
        assertTrue(validationService.validate(branchRequest("feature/PAY-1234-api-tests", tempDir)).isEmpty());
        assertTrue(validationService.validate(branchRequest("main", tempDir)).isEmpty());
        assertTrue(validationService.validate(branchRequest("release-1.0", tempDir)).isEmpty());
    }

    @Test
    void rejectsUnsafeBranchNames() throws Exception {
        List<String> errors = validationService.validate(branchRequest("feature; rm -rf", tempDir));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("newBranchName")));

        errors = validationService.validate(branchRequest("../escape", tempDir));
        assertTrue(errors.stream().anyMatch(e -> e.contains("newBranchName")));

        errors = validationService.validate(branchRequest("feature//bad", tempDir));
        assertTrue(errors.stream().anyMatch(e -> e.contains("newBranchName")));
    }

    @Test
    void rejectsUnsafeRemoteNames() throws Exception {
        GitPrRequest request = branchRequest("feature/PAY-1234", tempDir);
        request.setRemoteName("https://github.com/org/repo.git");
        List<String> errors = validationService.validate(request);
        assertTrue(errors.stream().anyMatch(e -> e.contains("remoteName")));
    }

    @Test
    void rejectsAbsoluteAndTraversalFilePaths() throws Exception {
        Path project = createProjectWithAllowedFile();

        GitPrRequest request = baseRequest(project);
        request.setFilesToCommit(List.of("/etc/passwd"));
        List<String> errors = validationService.validate(request);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Absolute paths")));

        request.setFilesToCommit(List.of("src/test/../main/Evil.java"));
        errors = validationService.validate(request);
        assertTrue(errors.stream().anyMatch(e -> e.contains("traversal") || e.contains("outside")));
    }

    @Test
    void rejectsFilesOutsideAllowedPrefixes() throws Exception {
        Path project = createProjectWithAllowedFile();

        GitPrRequest request = baseRequest(project);
        request.setFilesToCommit(List.of("src/main/java/App.java"));
        List<String> errors = validationService.validate(request);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Blocked file path") || e.contains("outside allowed")));
    }

    @Test
    void appliesDefaultsFromJiraKey() {
        GitPrRequest request = new GitPrRequest();
        request.setProjectPath(tempDir.toString());
        request.setJiraStoryKey("PAY-999");
        request.setFilesToCommit(List.of("src/test/resources/features/payment/x.feature"));

        validationService.applyDefaults(request);

        assertEquals("main", request.getBaseBranch());
        assertEquals("origin", request.getRemoteName());
        assertEquals("feature/PAY-999-api-tests", request.getNewBranchName());
        assertEquals("Add API automation tests for PAY-999", request.getCommitMessage());
        assertEquals("PAY-999 Add API automation tests", request.getPrTitle());
    }

    private GitPrRequest branchRequest(String branchName, Path project) throws Exception {
        GitPrRequest request = baseRequest(project);
        request.setNewBranchName(branchName);
        return request;
    }

    private GitPrRequest baseRequest(Path project) throws Exception {
        Path feature = project.resolve("src/test/resources/features/payment/create_payment.feature");
        Files.createDirectories(feature.getParent());
        Files.writeString(feature, "Feature: payment");

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

    private Path createProjectWithAllowedFile() throws Exception {
        Path feature = tempDir.resolve("src/test/resources/features/payment/create_payment.feature");
        Files.createDirectories(feature.getParent());
        Files.writeString(feature, "Feature: payment");
        return tempDir;
    }
}
