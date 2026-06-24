package com.agentic.api.service;

import com.agentic.api.model.FileWriteRequest;
import com.agentic.api.model.FileWriteResponse;
import com.agentic.api.model.FileWriteResult;
import com.agentic.api.model.GeneratedFileDto;
import com.agentic.api.support.TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileWriteServiceTest {

    @TempDir
    Path tempDir;

    private FileWriteService fileWriteService;
    private Path projectRoot;

    @BeforeEach
    void setUp() throws Exception {
        fileWriteService = new FileWriteService(
                TestSupport.permissivePathPolicy(),
                TestSupport.operationConfirmationService(),
                TestSupport.mockRunHistoryService(),
                TestSupport.secretMaskingService()
        );
        projectRoot = tempDir.resolve("automation-project");
        Files.createDirectories(projectRoot.resolve("src/test"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
    }

    @Test
    void previewCreateNewFile() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("src/test/resources/features/payment/create_payment.feature", "Feature: Payment")
        ));

        assertEquals(1, response.getSummary().getCreate());
        assertEquals(0, response.getSummary().getBlocked());
        FileWriteResult result = response.getResults().get(0);
        assertEquals("CREATE", result.getAction());
        assertEquals("READY", result.getStatus());
        assertTrue(result.getDiff().contains("+++ src/test/resources/features/payment/create_payment.feature"));
        assertFalse(Files.exists(Path.of(result.getAbsolutePath())));
    }

    @Test
    void writeCreatesNewFile() throws Exception {
        FileWriteResponse response = fileWriteService.writeGeneratedFiles(request(
                file("src/test/resources/features/payment/create_payment.feature", "Feature: Payment")
        ));

        assertEquals(1, response.getSummary().getWritten());
        FileWriteResult result = response.getResults().get(0);
        assertEquals("WRITTEN", result.getStatus());
        assertEquals("Feature: Payment", Files.readString(Path.of(result.getAbsolutePath()), StandardCharsets.UTF_8));
    }

    @Test
    void skipExistingWhenOverwriteDisabled() throws Exception {
        Path target = projectRoot.resolve("src/test/java/steps/payment/PaymentSteps.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "old content", StandardCharsets.UTF_8);

        FileWriteResponse preview = fileWriteService.previewFileWrite(request(
                file("src/test/java/steps/payment/PaymentSteps.java", "new content")
        ));
        FileWriteResponse write = fileWriteService.writeGeneratedFiles(request(
                file("src/test/java/steps/payment/PaymentSteps.java", "new content")
        ));

        assertEquals("SKIP", preview.getResults().get(0).getAction());
        assertEquals("SKIPPED", write.getResults().get(0).getStatus());
        assertEquals("old content", Files.readString(target, StandardCharsets.UTF_8));
        assertEquals(1, write.getSummary().getSkip());
    }

    @Test
    void updateExistingWhenOverwriteEnabled() throws Exception {
        Path target = projectRoot.resolve("src/test/java/api/payment/PaymentApiClient.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "old content", StandardCharsets.UTF_8);

        FileWriteRequest request = request(
                file("src/test/java/api/payment/PaymentApiClient.java", "new content")
        );
        request.setOverwriteExisting(true);
        request.setCreateBackup(false);

        FileWriteResponse preview = fileWriteService.previewFileWrite(request);
        FileWriteResponse write = fileWriteService.writeGeneratedFiles(request);

        assertEquals("UPDATE", preview.getResults().get(0).getAction());
        assertEquals("READY", preview.getResults().get(0).getStatus());
        assertEquals("WRITTEN", write.getResults().get(0).getStatus());
        assertEquals("new content", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void backupCreatedWhenCreateBackupEnabled() throws Exception {
        Path target = projectRoot.resolve("src/test/java/validators/payment/PaymentValidator.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "old content", StandardCharsets.UTF_8);

        FileWriteRequest request = request(
                file("src/test/java/validators/payment/PaymentValidator.java", "new content")
        );
        request.setOverwriteExisting(true);
        request.setCreateBackup(true);

        FileWriteResponse response = fileWriteService.writeGeneratedFiles(request);
        FileWriteResult result = response.getResults().get(0);

        assertNotNull(result.getBackupPath());
        assertEquals("old content", Files.readString(Path.of(result.getBackupPath()), StandardCharsets.UTF_8));
        assertEquals("new content", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void blockPathTraversal() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("src/test/resources/features/../../.env", "secret")
        ));

        assertEquals(1, response.getSummary().getBlocked());
        assertEquals("BLOCKED", response.getResults().get(0).getStatus());
    }

    @Test
    void blockAbsolutePath() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("/etc/passwd", "bad")
        ));

        assertEquals("BLOCKED", response.getResults().get(0).getStatus());
    }

    @Test
    void blockDriveLetterPath() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("C:/secrets/key.pem", "bad")
        ));

        assertEquals("BLOCKED", response.getResults().get(0).getStatus());
    }

    @Test
    void blockSensitiveFiles() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("src/test/resources/features/payment/cert.pem", "secret"),
                file(".git/config", "bad"),
                file("src/test/resources/templates/payment/.env", "bad")
        ));

        assertEquals(3, response.getSummary().getBlocked());
    }

    @Test
    void blockFilesOutsideAllowedPrefixes() {
        FileWriteResponse response = fileWriteService.previewFileWrite(request(
                file("src/main/java/App.java", "bad")
        ));

        assertEquals("BLOCKED", response.getResults().get(0).getStatus());
        assertTrue(response.getResults().get(0).getMessage().contains("allowed test automation folders"));
    }

    @Test
    void blockWhenProjectPathMissing() {
        FileWriteRequest request = request(
                file("src/test/resources/features/payment/create_payment.feature", "Feature")
        );
        request.setProjectPath(tempDir.resolve("missing-project").toString());

        FileWriteResponse response = fileWriteService.previewFileWrite(request);

        assertFalse(response.getErrors().isEmpty());
        assertEquals(1, response.getSummary().getBlocked());
    }

    @Test
    void warnWhenAutomationMarkersMissing() throws Exception {
        Path bareProject = tempDir.resolve("bare-project");
        Files.createDirectories(bareProject);

        FileWriteRequest request = request(
                file("src/test/resources/features/payment/create_payment.feature", "Feature")
        );
        request.setProjectPath(bareProject.toString());

        FileWriteResponse response = fileWriteService.previewFileWrite(request);

        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> w.contains("No automation project markers found")));
        assertEquals("CREATE", response.getResults().get(0).getAction());
    }

    @Test
    void updateDiffShowsChangedLines() {
        String diff = fileWriteService.buildUpdateDiff("line1\nold\nline3", "line1\nnew\nline3");
        assertTrue(diff.contains("-old"));
        assertTrue(diff.contains("+new"));
        assertFalse(diff.contains("-line1"));
    }

    private FileWriteRequest request(GeneratedFileDto... files) {
        FileWriteRequest request = new FileWriteRequest();
        request.setProjectPath(projectRoot.toString());
        request.setFiles(List.of(files));
        request.setWriteMode("preview");
        return request;
    }

    private GeneratedFileDto file(String path, String content) {
        return new GeneratedFileDto(path, content, "text");
    }
}
