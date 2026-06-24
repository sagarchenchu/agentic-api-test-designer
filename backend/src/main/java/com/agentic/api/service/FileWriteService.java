package com.agentic.api.service;

import com.agentic.api.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FileWriteService {

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "src/test/resources/features/",
            "src/test/resources/templates/",
            "src/test/resources/testdata/",
            "src/test/resources/schemas/",
            "src/test/java/steps/",
            "src/test/java/api/",
            "src/test/java/validators/"
    );

    private static final List<String> BLOCKED_PATH_SEGMENTS = List.of(
            ".git/",
            ".github/"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".keystore", ".p12", ".pem", ".key"
    );

    private static final Pattern DRIVE_LETTER_PATTERN = Pattern.compile("^[A-Za-z]:");
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final ProjectPathPolicyService projectPathPolicyService;
    private final OperationConfirmationService operationConfirmationService;
    private final RunHistoryService runHistoryService;
    private final SecretMaskingService secretMaskingService;

    public FileWriteService(
            ProjectPathPolicyService projectPathPolicyService,
            OperationConfirmationService operationConfirmationService,
            RunHistoryService runHistoryService,
            SecretMaskingService secretMaskingService
    ) {
        this.projectPathPolicyService = projectPathPolicyService;
        this.operationConfirmationService = operationConfirmationService;
        this.runHistoryService = runHistoryService;
        this.secretMaskingService = secretMaskingService;
    }

    public FileWriteResponse previewFileWrite(FileWriteRequest request) {
        return process(request, false);
    }

    public FileWriteResponse writeGeneratedFiles(FileWriteRequest request) {
        operationConfirmationService.requireConfirmation(request.getConfirmation());
        FileWriteResponse response = process(request, true);
        runHistoryService.recordFileWrite(response, null);
        maskResponse(response);
        return response;
    }

    private FileWriteResponse process(FileWriteRequest request, boolean writeMode) {
        FileWriteResponse response = new FileWriteResponse();
        response.setProjectPath(request.getProjectPath());

        Path projectRoot = validateProjectPath(request.getProjectPath(), response);
        if (projectRoot == null) {
            blockAllFiles(request, response, "Project path validation failed");
            buildSummary(response);
            return response;
        }

        for (GeneratedFileDto file : request.getFiles()) {
            response.getResults().add(processFile(projectRoot, file, request, writeMode, response));
        }

        buildSummary(response);
        return response;
    }

    private Path validateProjectPath(String projectPath, FileWriteResponse response) {
        if (projectPath == null || projectPath.isBlank()) {
            response.getErrors().add("projectPath is required");
            return null;
        }

        response.getErrors().addAll(secretMaskingService.maskList(
                projectPathPolicyService.validateProjectPath(projectPath)));
        if (!response.getErrors().isEmpty()) {
            return null;
        }

        Path root = Paths.get(projectPath.trim()).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            response.getErrors().add("projectPath does not exist: " + root);
            return null;
        }
        if (!Files.isDirectory(root)) {
            response.getErrors().add("projectPath is not a directory: " + root);
            return null;
        }

        if (!hasAutomationMarkers(root)) {
            response.getWarnings().add(
                    "No automation project markers found (pom.xml, build.gradle, settings.gradle, or src/test). "
                            + "Proceed with caution."
            );
        }

        return root;
    }

    private boolean hasAutomationMarkers(Path root) {
        return Files.exists(root.resolve("pom.xml"))
                || Files.exists(root.resolve("build.gradle"))
                || Files.exists(root.resolve("settings.gradle"))
                || Files.exists(root.resolve("src/test"));
    }

    private void blockAllFiles(FileWriteRequest request, FileWriteResponse response, String message) {
        for (GeneratedFileDto file : request.getFiles()) {
            FileWriteResult result = new FileWriteResult();
            result.setRelativePath(file.getPath());
            result.setAbsolutePath(null);
            result.setAction("BLOCKED");
            result.setStatus("BLOCKED");
            result.setMessage(message);
            response.getResults().add(result);
        }
    }

    private FileWriteResult processFile(
            Path projectRoot,
            GeneratedFileDto file,
            FileWriteRequest request,
            boolean writeMode,
            FileWriteResponse response
    ) {
        FileWriteResult result = new FileWriteResult();
        result.setRelativePath(file.getPath());

        String pathError = validateRelativePath(file.getPath());
        if (pathError != null) {
            return blockedResult(result, projectRoot, file.getPath(), pathError);
        }

        String normalizedRelative = normalizeRelativePath(file.getPath());
        result.setRelativePath(normalizedRelative);

        if (!isAllowedPrefix(normalizedRelative)) {
            return blockedResult(
                    result,
                    projectRoot,
                    normalizedRelative,
                    "Path is outside allowed test automation folders"
            );
        }

        if (isBlockedPath(normalizedRelative)) {
            return blockedResult(
                    result,
                    projectRoot,
                    normalizedRelative,
                    "Path targets a blocked or sensitive location"
            );
        }

        Path target = projectRoot.resolve(normalizedRelative).normalize();
        if (!target.startsWith(projectRoot)) {
            return blockedResult(
                    result,
                    projectRoot,
                    normalizedRelative,
                    "Resolved path escapes projectPath"
            );
        }

        result.setAbsolutePath(target.toString());

        String newContent = file.getContent() != null ? file.getContent() : "";
        boolean exists = Files.exists(target);
        String existingContent = "";
        if (exists && Files.isRegularFile(target)) {
            try {
                existingContent = Files.readString(target, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                return errorResult(result, "Failed to read existing file: " + ex.getMessage());
            }
        } else if (exists) {
            return blockedResult(result, projectRoot, normalizedRelative, "Target path exists but is not a regular file");
        }

        if (!exists) {
            result.setAction("CREATE");
            result.setDiff(buildCreateDiff(normalizedRelative, newContent));
            if (writeMode) {
                return writeCreate(result, target, newContent);
            }
            result.setStatus("READY");
            result.setMessage("File does not exist and will be created");
            return result;
        }

        if (!request.isOverwriteExisting()) {
            result.setAction("SKIP");
            result.setStatus(writeMode ? "SKIPPED" : "SKIPPED");
            result.setMessage("Existing file not overwritten because overwriteExisting=false");
            result.setDiff(buildUpdateDiff(existingContent, newContent));
            return result;
        }

        result.setAction("UPDATE");
        result.setDiff(buildUpdateDiff(existingContent, newContent));
        if (writeMode) {
            return writeUpdate(result, target, existingContent, newContent, request.isCreateBackup());
        }
        result.setStatus("READY");
        result.setMessage("Existing file will be updated");
        return result;
    }

    private FileWriteResult writeCreate(FileWriteResult result, Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            result.setStatus("WRITTEN");
            result.setMessage("File created successfully");
            return result;
        } catch (IOException ex) {
            return errorResult(result, "Failed to create file: " + ex.getMessage());
        }
    }

    private FileWriteResult writeUpdate(
            FileWriteResult result,
            Path target,
            String existingContent,
            String newContent,
            boolean createBackup
    ) {
        try {
            if (createBackup) {
                String backupName = target.getFileName() + ".bak." + BACKUP_TIMESTAMP.format(LocalDateTime.now());
                Path backupPath = target.resolveSibling(backupName);
                Files.writeString(backupPath, existingContent, StandardCharsets.UTF_8);
                result.setBackupPath(backupPath.toString());
            }
            Files.writeString(target, newContent, StandardCharsets.UTF_8);
            result.setStatus("WRITTEN");
            result.setMessage(createBackup
                    ? "File updated successfully with backup"
                    : "File updated successfully");
            return result;
        } catch (IOException ex) {
            return errorResult(result, "Failed to update file: " + ex.getMessage());
        }
    }

    private FileWriteResult blockedResult(
            FileWriteResult result,
            Path projectRoot,
            String relativePath,
            String message
    ) {
        result.setRelativePath(relativePath);
        if (relativePath != null && !relativePath.isBlank()) {
            result.setAbsolutePath(projectRoot.resolve(normalizeRelativePath(relativePath)).normalize().toString());
        }
        result.setAction("BLOCKED");
        result.setStatus("BLOCKED");
        result.setMessage(message);
        return result;
    }

    private FileWriteResult errorResult(FileWriteResult result, String message) {
        result.setAction(result.getAction() != null ? result.getAction() : "BLOCKED");
        result.setStatus("ERROR");
        result.setMessage(message);
        return result;
    }

    String validateRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return "File path is required";
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
            return "Absolute paths are not allowed";
        }
        if (DRIVE_LETTER_PATTERN.matcher(trimmed).find()) {
            return "Drive-letter paths are not allowed";
        }
        if (trimmed.contains("..")) {
            return "Path traversal is not allowed";
        }
        return null;
    }

    String normalizeRelativePath(String path) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return Paths.get(normalized).normalize().toString().replace('\\', '/');
    }

    boolean isAllowedPrefix(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            return false;
        }
        return ALLOWED_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    boolean isBlockedPath(String relativePath) {
        String normalized = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.equals(".env") || normalized.endsWith("/.env")) {
            return true;
        }
        for (String segment : BLOCKED_PATH_SEGMENTS) {
            if (normalized.contains(segment)) {
                return true;
            }
        }
        for (String extension : BLOCKED_EXTENSIONS) {
            if (normalized.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    String buildCreateDiff(String relativePath, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- /dev/null\n");
        sb.append("+++ ").append(relativePath).append("\n");
        sb.append("@@\n");
        for (String line : content.split("\n", -1)) {
            sb.append("+").append(line).append("\n");
        }
        return sb.toString().trim();
    }

    String buildUpdateDiff(String existingContent, String newContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- existing\n");
        sb.append("+++ generated\n");
        sb.append("@@\n");

        String[] oldLines = existingContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);
        int max = Math.max(oldLines.length, newLines.length);
        for (int i = 0; i < max; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;
            if (oldLine != null && newLine != null && oldLine.equals(newLine)) {
                continue;
            }
            if (oldLine != null) {
                sb.append("-").append(oldLine).append("\n");
            }
            if (newLine != null) {
                sb.append("+").append(newLine).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void buildSummary(FileWriteResponse response) {
        FileWriteSummary summary = new FileWriteSummary();
        summary.setTotal(response.getResults().size());

        int create = 0;
        int update = 0;
        int skip = 0;
        int blocked = 0;
        int written = 0;
        int errors = 0;

        for (FileWriteResult result : response.getResults()) {
            switch (result.getAction()) {
                case "CREATE" -> create++;
                case "UPDATE" -> update++;
                case "SKIP" -> skip++;
                case "BLOCKED" -> blocked++;
                default -> {
                }
            }
            switch (result.getStatus()) {
                case "WRITTEN" -> written++;
                case "ERROR" -> errors++;
                default -> {
                }
            }
        }

        summary.setCreate(create);
        summary.setUpdate(update);
        summary.setSkip(skip);
        summary.setBlocked(blocked);
        summary.setWritten(written);
        summary.setErrors(errors);
        response.setSummary(summary);
    }

    private void maskResponse(FileWriteResponse response) {
        response.setWarnings(secretMaskingService.maskList(response.getWarnings()));
        response.setErrors(secretMaskingService.maskList(response.getErrors()));
    }
}
