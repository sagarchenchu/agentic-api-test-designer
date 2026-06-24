package com.agentic.api.service;

import com.agentic.api.model.GitPrRequest;
import com.agentic.api.model.GitPrResponse;
import com.agentic.api.service.GitProcessRunnerService.GitCommandResult;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GitPrService {

    private static final String GH_MISSING_MESSAGE =
            "GitHub CLI not found. Install gh or configure Phase 8 GitHub API integration.";

    private static final DateTimeFormatter OPERATION_ID_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final Map<String, GitPrResponse> operations = new ConcurrentHashMap<>();
    private final AtomicInteger operationSequence = new AtomicInteger(0);

    private final GitValidationService validationService;
    private final GitCommandBuilder commandBuilder;
    private final GitProcessRunnerService processRunnerService;
    private final GitStatusParserService statusParserService;
    private final OperationConfirmationService operationConfirmationService;
    private final RunHistoryService runHistoryService;
    private final SecretMaskingService secretMaskingService;

    public GitPrService(
            GitValidationService validationService,
            GitCommandBuilder commandBuilder,
            GitProcessRunnerService processRunnerService,
            GitStatusParserService statusParserService,
            OperationConfirmationService operationConfirmationService,
            RunHistoryService runHistoryService,
            SecretMaskingService secretMaskingService
    ) {
        this.validationService = validationService;
        this.commandBuilder = commandBuilder;
        this.processRunnerService = processRunnerService;
        this.statusParserService = statusParserService;
        this.operationConfirmationService = operationConfirmationService;
        this.runHistoryService = runHistoryService;
        this.secretMaskingService = secretMaskingService;
    }

    public GitPrResponse previewGitPr(GitPrRequest request) {
        GitPrResponse response = buildBaseResponse(request);
        List<String> commandLog = new ArrayList<>();

        List<String> validationErrors = validationService.validate(request);
        if (!validationErrors.isEmpty()) {
            response.getErrors().addAll(validationErrors);
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        Path projectRoot = Paths.get(request.getProjectPath().trim()).toAbsolutePath().normalize();
        List<String> files = validationService.normalizeFiles(request.getFilesToCommit());

        GitCommandResult topLevel = runGit(commandBuilder.revParseTopLevel(), projectRoot, commandLog);
        if (!topLevel.isSuccess()) {
            response.getErrors().add("projectPath is not a git repository");
            if (!topLevel.output().isBlank()) {
                response.getErrors().add(topLevel.output());
            }
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        GitCommandResult statusResult = runGit(commandBuilder.statusShort(), projectRoot, commandLog);
        if (!statusResult.isSuccess()) {
            response.getErrors().add("git status failed");
            if (!statusResult.output().isBlank()) {
                response.getErrors().add(statusResult.output());
            }
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        Map<String, String> statusByPath = statusParserService.parseStatusShort(statusResult.output());
        response.setChangedFiles(statusParserService.matchingChangedFiles(statusByPath, files));
        addMissingStatusWarnings(files, statusByPath, response.getWarnings());

        commandLog.addAll(commandBuilder.formatCommands(plannedCreateCommands(request, files)));
        response.setCommandLog(commandLog);
        response.setStatus(response.getErrors().isEmpty() ? "READY" : "ERROR");
        store(response);
        return response;
    }

    public GitPrResponse createGitPr(GitPrRequest request) {
        operationConfirmationService.requireConfirmation(request.getConfirmation());
        GitPrResponse response = buildBaseResponse(request);
        List<String> commandLog = new ArrayList<>();

        List<String> validationErrors = validationService.validate(request);
        if (!validationErrors.isEmpty()) {
            response.getErrors().addAll(validationErrors);
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        Path projectRoot = Paths.get(request.getProjectPath().trim()).toAbsolutePath().normalize();
        List<String> files = validationService.normalizeFiles(request.getFilesToCommit());

        GitCommandResult topLevel = runGit(commandBuilder.revParseTopLevel(), projectRoot, commandLog);
        if (!topLevel.isSuccess()) {
            response.getErrors().add("projectPath is not a git repository");
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        GitCommandResult statusResult = runGit(commandBuilder.statusShort(), projectRoot, commandLog);
        if (!statusResult.isSuccess()) {
            response.getErrors().add("git status failed");
            response.setStatus("ERROR");
            store(response);
            return response;
        }

        Map<String, String> statusByPath = statusParserService.parseStatusShort(statusResult.output());
        List<String> unrelated = statusParserService.findUnrelatedChanges(statusByPath, files);
        if (!unrelated.isEmpty()) {
            response.getErrors().add("Working tree has unrelated changes outside filesToCommit: "
                    + String.join(", ", unrelated));
            response.setStatus("FAILED");
            response.setCommandLog(commandLog);
            store(response);
            return response;
        }

        response.setChangedFiles(statusParserService.matchingChangedFiles(statusByPath, files));

        if (request.isDryRun()) {
            commandLog.addAll(commandBuilder.formatCommands(plannedCreateCommands(request, files)));
            response.setCommandLog(commandLog);
            response.getWarnings().add("dryRun=true; git repository was not modified");
            response.setStatus("READY");
            store(response);
            return response;
        }

        GitCommandResult ghVersion = processRunnerService.run(commandBuilder.ghVersion(), projectRoot);
        if (!ghVersion.isSuccess()) {
            response.getErrors().add(GH_MISSING_MESSAGE);
            response.setStatus("FAILED");
            response.setCommandLog(commandLog);
            store(response);
            return response;
        }

        if (!runStep(commandBuilder.checkout(request.getBaseBranch()), projectRoot, commandLog, response)) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        if (!runStep(
                commandBuilder.pull(request.getRemoteName(), request.getBaseBranch()),
                projectRoot,
                commandLog,
                response
        )) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        if (!runStep(commandBuilder.checkoutNewBranch(request.getNewBranchName()), projectRoot, commandLog, response)) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        if (!runStep(commandBuilder.add(files), projectRoot, commandLog, response)) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        if (!runStep(commandBuilder.commit(request.getCommitMessage()), projectRoot, commandLog, response)) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        GitCommandResult head = runGit(commandBuilder.revParseHead(), projectRoot, commandLog);
        if (head.isSuccess()) {
            response.setCommitSha(head.firstLine());
        }

        if (!runStep(
                commandBuilder.push(request.getRemoteName(), request.getNewBranchName()),
                projectRoot,
                commandLog,
                response
        )) {
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        List<String> ghCommand = commandBuilder.ghPrCreate(
                request.getBaseBranch(),
                request.getNewBranchName(),
                request.getPrTitle(),
                request.getPrBody()
        );
        GitCommandResult prResult = runGit(ghCommand, projectRoot, commandLog);
        if (!prResult.isSuccess()) {
            response.getErrors().add("GitHub PR creation failed");
            if (prResult.getException() != null) {
                response.getErrors().add(prResult.getException());
            }
            if (!prResult.output().isBlank()) {
                response.getErrors().add(prResult.output());
            }
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        String prUrl = statusParserService.parsePrUrl(prResult.getOutputLines());
        if (prUrl == null || prUrl.isBlank()) {
            response.getErrors().add("Could not parse PR URL from gh output");
            response.setStatus("FAILED");
            store(response);
            return response;
        }

        response.setPrUrl(prUrl);
        response.setCommandLog(commandLog);
        response.setStatus("CREATED");
        maskResponse(response);
        store(response);
        runHistoryService.recordGitPr(response, request.getJiraStoryKey());
        return response;
    }

    public GitPrResponse getGitPr(String operationId) {
        GitPrResponse response = operations.get(operationId);
        if (response == null) {
            throw new NoSuchElementException("Git PR operation not found: " + operationId);
        }
        return response;
    }

    private GitPrResponse buildBaseResponse(GitPrRequest request) {
        validationService.applyDefaults(request);
        GitPrResponse response = new GitPrResponse();
        response.setOperationId(nextOperationId());
        response.setProjectPath(request.getProjectPath());
        response.setBaseBranch(request.getBaseBranch());
        response.setNewBranchName(request.getNewBranchName());
        return response;
    }

    private String nextOperationId() {
        int sequence = operationSequence.incrementAndGet();
        return "gitpr-" + OPERATION_ID_DATE.format(Instant.now()) + "-" + String.format("%03d", sequence);
    }

    private List<List<String>> plannedCreateCommands(GitPrRequest request, List<String> files) {
        List<List<String>> commands = new ArrayList<>();
        commands.add(commandBuilder.checkout(request.getBaseBranch()));
        commands.add(commandBuilder.pull(request.getRemoteName(), request.getBaseBranch()));
        commands.add(commandBuilder.checkoutNewBranch(request.getNewBranchName()));
        commands.add(commandBuilder.add(files));
        commands.add(commandBuilder.commit(request.getCommitMessage()));
        commands.add(commandBuilder.revParseHead());
        commands.add(commandBuilder.push(request.getRemoteName(), request.getNewBranchName()));
        commands.add(commandBuilder.ghPrCreate(
                request.getBaseBranch(),
                request.getNewBranchName(),
                request.getPrTitle(),
                request.getPrBody()
        ));
        return commands;
    }

    private void addMissingStatusWarnings(
            List<String> files,
            Map<String, String> statusByPath,
            List<String> warnings
    ) {
        for (String file : files) {
            if (!statusByPath.containsKey(file)) {
                warnings.add("File has no git status entry (may be unchanged or untracked): " + file);
            }
        }
    }

    private GitCommandResult runGit(List<String> command, Path projectRoot, List<String> commandLog) {
        commandLog.add(commandBuilder.formatCommand(command));
        return processRunnerService.run(command, projectRoot);
    }

    private boolean runStep(
            List<String> command,
            Path projectRoot,
            List<String> commandLog,
            GitPrResponse response
    ) {
        GitCommandResult result = runGit(command, projectRoot, commandLog);
        if (!result.isSuccess()) {
            response.getErrors().add("Command failed: " + commandBuilder.formatCommand(command));
            if (result.getException() != null) {
                response.getErrors().add(result.getException());
            }
            if (!result.output().isBlank()) {
                response.getErrors().add(result.output());
            }
            response.setCommandLog(commandLog);
            return false;
        }
        return true;
    }

    private void store(GitPrResponse response) {
        operations.put(response.getOperationId(), response);
    }

    private void maskResponse(GitPrResponse response) {
        response.setCommandLog(secretMaskingService.maskList(response.getCommandLog()));
        response.setWarnings(secretMaskingService.maskList(response.getWarnings()));
        response.setErrors(secretMaskingService.maskList(response.getErrors()));
    }
}
