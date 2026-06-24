package com.agentic.api.service;

import com.agentic.api.model.GitPrRequest;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GitValidationService {

    private static final Pattern BRANCH_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");
    private static final Pattern REMOTE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern UNSAFE_BRANCH_CHARS = Pattern.compile(
            "(?i)(\\s|;|&&|\\|\\||[|><`$]|\\.\\.|~|\\^|:|\\?|\\*|\\[|\\\\)"
    );

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "src/test/resources/features/",
            "src/test/resources/templates/",
            "src/test/resources/testdata/",
            "src/test/resources/schemas/",
            "src/test/java/steps/",
            "src/test/java/api/",
            "src/test/java/validators/"
    );

    private static final Set<String> BLOCKED_EXACT = Set.of(
            ".git", ".github", ".env", "pom.xml", "build.gradle", "settings.gradle"
    );

    private static final Pattern DRIVE_LETTER_PATTERN = Pattern.compile("^[A-Za-z]:");

    private final ProjectPathPolicyService projectPathPolicyService;

    public GitValidationService(ProjectPathPolicyService projectPathPolicyService) {
        this.projectPathPolicyService = projectPathPolicyService;
    }

    public void applyDefaults(GitPrRequest request) {
        String jiraKey = safe(request.getJiraStoryKey());
        if (jiraKey.isBlank()) {
            jiraKey = "STORY-000";
        }
        if (request.getBaseBranch() == null || request.getBaseBranch().isBlank()) {
            request.setBaseBranch("main");
        }
        if (request.getRemoteName() == null || request.getRemoteName().isBlank()) {
            request.setRemoteName("origin");
        }
        if (request.getNewBranchName() == null || request.getNewBranchName().isBlank()) {
            request.setNewBranchName("feature/" + jiraKey + "-api-tests");
        }
        if (request.getCommitMessage() == null || request.getCommitMessage().isBlank()) {
            request.setCommitMessage("Add API automation tests for " + jiraKey);
        }
        if (request.getPrTitle() == null || request.getPrTitle().isBlank()) {
            request.setPrTitle(jiraKey + " Add API automation tests");
        }
        if (request.getPrBody() == null || request.getPrBody().isBlank()) {
            request.setPrBody("Generated API tests from Jira + Swagger.");
        }
    }

    public List<String> validate(GitPrRequest request) {
        applyDefaults(request);
        List<String> errors = new ArrayList<>();

        errors.addAll(projectPathPolicyService.validateProjectPath(request.getProjectPath()));
        Path projectRoot = validateProjectPath(request.getProjectPath(), errors);
        validateBranch(request.getBaseBranch(), "baseBranch", errors);
        validateBranch(request.getNewBranchName(), "newBranchName", errors);
        validateRemote(request.getRemoteName(), errors);
        validateText(request.getCommitMessage(), "commitMessage", 200, errors);
        validateText(request.getPrTitle(), "prTitle", 200, errors);
        validateText(request.getPrBody(), "prBody", 5000, errors);

        if (request.getFilesToCommit() == null || request.getFilesToCommit().isEmpty()) {
            errors.add("filesToCommit must not be empty");
        } else {
            for (String file : request.getFilesToCommit()) {
                validateFilePath(file, errors);
            }
        }

        if (projectRoot != null) {
            for (String file : request.getFilesToCommit()) {
                String normalized = normalizeRelativePath(file);
                Path target = projectRoot.resolve(normalized).normalize();
                if (!target.startsWith(projectRoot)) {
                    errors.add("File path escapes projectPath: " + file);
                } else if (!Files.exists(target)) {
                    errors.add("File does not exist: " + normalized);
                }
            }
        }

        return errors;
    }

    Path validateProjectPath(String projectPath, List<String> errors) {
        if (projectPath == null || projectPath.isBlank()) {
            errors.add("projectPath is required");
            return null;
        }
        Path root = Paths.get(projectPath.trim()).toAbsolutePath().normalize();
        if (root.getParent() == null || root.equals(root.getRoot())) {
            errors.add("projectPath must not be a filesystem root");
            return null;
        }
        if (!Files.exists(root)) {
            errors.add("projectPath does not exist: " + root);
            return null;
        }
        if (!Files.isDirectory(root)) {
            errors.add("projectPath is not a directory: " + root);
            return null;
        }
        return root;
    }

    void validateBranch(String branch, String field, List<String> errors) {
        if (branch == null || branch.isBlank()) {
            errors.add(field + " is required");
            return;
        }
        if (UNSAFE_BRANCH_CHARS.matcher(branch).find()) {
            errors.add(field + " contains unsafe characters");
            return;
        }
        if (!BRANCH_PATTERN.matcher(branch).matches()) {
            errors.add(field + " has invalid format");
            return;
        }
        if (branch.startsWith("/") || branch.endsWith("/") || branch.contains("//")) {
            errors.add(field + " must not start/end with / or contain //");
        }
        if (branch.contains("..")) {
            errors.add(field + " must not contain ..");
        }
    }

    void validateRemote(String remote, List<String> errors) {
        if (remote == null || remote.isBlank()) {
            errors.add("remoteName is required");
            return;
        }
        if (remote.contains("://") || remote.contains("/")) {
            errors.add("remoteName must not be a URL in Phase 8");
            return;
        }
        if (!REMOTE_PATTERN.matcher(remote).matches()) {
            errors.add("remoteName has invalid format");
        }
    }

    void validateText(String value, String field, int maxLength, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
            return;
        }
        if (value.contains("\u0000")) {
            errors.add(field + " must not contain null bytes");
        }
        if (value.length() > maxLength) {
            errors.add(field + " must be at most " + maxLength + " characters");
        }
    }

    void validateFilePath(String path, List<String> errors) {
        if (path == null || path.isBlank()) {
            errors.add("filesToCommit contains a blank path");
            return;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
            errors.add("Absolute paths are not allowed: " + path);
            return;
        }
        if (DRIVE_LETTER_PATTERN.matcher(trimmed).find()) {
            errors.add("Drive-letter paths are not allowed: " + path);
            return;
        }
        if (trimmed.contains("..")) {
            errors.add("Path traversal is not allowed: " + path);
            return;
        }

        String normalized = normalizeRelativePath(trimmed);
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (BLOCKED_EXACT.contains(lower)) {
            errors.add("Blocked file path: " + normalized);
            return;
        }
        if (lower.startsWith(".git/") || lower.startsWith(".github/") || lower.equals(".env")
                || lower.endsWith("/.env") || lower.startsWith("src/main/")) {
            errors.add("Blocked file path: " + normalized);
            return;
        }
        if (lower.equals("pom.xml") || lower.equals("build.gradle") || lower.equals("settings.gradle")) {
            errors.add("Blocked file path: " + normalized);
            return;
        }
        if (!ALLOWED_PREFIXES.stream().anyMatch(normalized::startsWith)) {
            errors.add("File is outside allowed automation folders: " + normalized);
        }
    }

    String normalizeRelativePath(String path) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return Paths.get(normalized).normalize().toString().replace('\\', '/');
    }

    List<String> normalizeFiles(List<String> files) {
        return files.stream().map(this::normalizeRelativePath).distinct().toList();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
