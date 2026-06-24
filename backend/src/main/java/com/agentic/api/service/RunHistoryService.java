package com.agentic.api.service;

import com.agentic.api.entity.AgentRunHistoryEntity;
import com.agentic.api.entity.ExternalOperationEntity;
import com.agentic.api.entity.RunArtifactEntity;
import com.agentic.api.model.*;
import com.agentic.api.repository.AgentRunHistoryRepository;
import com.agentic.api.repository.ExternalOperationRepository;
import com.agentic.api.repository.RunArtifactRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RunHistoryService {

    private final AgentRunHistoryRepository runHistoryRepository;
    private final RunArtifactRepository artifactRepository;
    private final ExternalOperationRepository externalOperationRepository;
    private final SecretMaskingService secretMaskingService;
    private final ObjectMapper objectMapper;

    public RunHistoryService(
            AgentRunHistoryRepository runHistoryRepository,
            RunArtifactRepository artifactRepository,
            ExternalOperationRepository externalOperationRepository,
            SecretMaskingService secretMaskingService,
            ObjectMapper objectMapper
    ) {
        this.runHistoryRepository = runHistoryRepository;
        this.artifactRepository = artifactRepository;
        this.externalOperationRepository = externalOperationRepository;
        this.secretMaskingService = secretMaskingService;
        this.objectMapper = objectMapper;
    }

    public String newRunId() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public AgentRunHistoryEntity recordAgentRun(AgentRequest request, AgentRunResponse response) {
        Instant now = Instant.now();
        AgentRunHistoryEntity entity = new AgentRunHistoryEntity();
        entity.setRunId(response.getRunId() != null ? response.getRunId() : newRunId());
        entity.setJiraStoryKey(request.getJiraStoryKey());
        entity.setSwaggerSourceType(resolveSwaggerSourceType(request));
        entity.setTestGenerationMode(request.getTestGenerationMode());
        entity.setFrameworkType(request.getFrameworkType());
        entity.setExecutionMode(request.getExecutionMode());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setStatus(response.getStatus());
        entity.setTestCaseCount(response.getTestCases() != null ? response.getTestCases().size() : 0);
        entity.setGeneratedFileCount(response.getGeneratedFiles() != null ? response.getGeneratedFiles().size() : 0);
        entity.setWarningsJson(writeJson(secretMaskingService.maskList(
                mergeLists(response.getTestMatrixWarnings(), List.of()))));
        entity.setErrorsJson(writeJson(List.of()));

        if (response.getGeneratedBdd() != null) {
            saveArtifact(entity.getRunId(), "BDD", response.getGeneratedBdd().getDownloadFilename(),
                    secretMaskingService.mask(response.getGeneratedBdd().getContent()));
        }
        if (response.getGeneratedFiles() != null) {
            for (GeneratedFileDto file : response.getGeneratedFiles()) {
                String content = file.getContent();
                if (content != null && content.length() > 9000) {
                    content = content.substring(0, 9000) + "\n... [truncated]";
                }
                saveArtifact(entity.getRunId(), "FILE", file.getPath(), secretMaskingService.mask(content));
            }
        }

        return runHistoryRepository.save(entity);
    }

    @Transactional
    public String recordFileWrite(FileWriteResponse response, String jiraStoryKey) {
        AgentRunHistoryEntity entity = createOperationRun(jiraStoryKey, "file-write");
        entity.setFileWriteSummary(secretMaskingService.mask(summaryText(response.getSummary())));
        entity.setWarningsJson(writeJson(secretMaskingService.maskList(response.getWarnings())));
        entity.setErrorsJson(writeJson(secretMaskingService.maskList(response.getErrors())));
        runHistoryRepository.save(entity);

        saveExternalOperation(entity.getRunId(), "FILE_WRITE", null,
                response.getSummary().getErrors() > 0 ? "ERROR" : "COMPLETED", null,
                writeJson(response.getSummary()));

        for (FileWriteResult result : response.getResults()) {
            if (result.getDiff() != null) {
                saveArtifact(entity.getRunId(), "FILE_DIFF", result.getRelativePath(),
                        secretMaskingService.mask(result.getDiff()));
            }
        }
        return entity.getRunId();
    }

    @Transactional
    public String recordTestExecution(TestExecutionResponse response, String jiraStoryKey) {
        AgentRunHistoryEntity entity = createOperationRun(jiraStoryKey, "test-execution");
        entity.setTestExecutionStatus(response.getStatus());
        entity.setWarningsJson(writeJson(secretMaskingService.maskList(response.getWarnings())));
        entity.setErrorsJson(writeJson(secretMaskingService.maskList(response.getErrors())));
        runHistoryRepository.save(entity);

        saveExternalOperation(entity.getRunId(), "TEST_EXECUTION", response.getExecutionId(),
                response.getStatus(), null, secretMaskingService.mask(response.getCommand()));

        if (response.getLogTail() != null) {
            saveArtifact(entity.getRunId(), "LOG_TAIL", response.getExecutionId(),
                    secretMaskingService.mask(response.getLogTail()));
        }
        return entity.getRunId();
    }

    @Transactional
    public String recordGitPr(GitPrResponse response, String jiraStoryKey) {
        AgentRunHistoryEntity entity = createOperationRun(jiraStoryKey, "git-pr");
        entity.setGitPrUrl(response.getPrUrl());
        entity.setStatus(response.getStatus());
        entity.setWarningsJson(writeJson(secretMaskingService.maskList(response.getWarnings())));
        entity.setErrorsJson(writeJson(secretMaskingService.maskList(response.getErrors())));
        runHistoryRepository.save(entity);

        saveExternalOperation(entity.getRunId(), "GIT_PR", response.getOperationId(),
                response.getStatus(), response.getPrUrl(),
                writeJson(secretMaskingService.maskList(response.getCommandLog())));
        return entity.getRunId();
    }

    @Transactional
    public String recordJiraOperation(JiraOperationResponse response, String operationType) {
        AgentRunHistoryEntity entity = createOperationRun(response.getJiraStoryKey(), "jira-" + operationType.toLowerCase());
        entity.setJiraUpdateStatus(response.getStatus());
        entity.setWarningsJson(writeJson(secretMaskingService.maskList(response.getWarnings())));
        entity.setErrorsJson(writeJson(secretMaskingService.maskList(response.getErrors())));
        runHistoryRepository.save(entity);

        saveExternalOperation(entity.getRunId(), operationType, response.getJiraStoryKey(),
                response.getStatus(), response.getUrl(), response.getMessage());
        return entity.getRunId();
    }

    public List<RunHistorySummaryDto> listRuns() {
        return runHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public RunHistoryDetailDto getRun(String runId) {
        AgentRunHistoryEntity entity = runHistoryRepository.findById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run history not found: " + runId));
        RunHistoryDetailDto detail = new RunHistoryDetailDto();
        copySummary(entity, detail);
        detail.setSwaggerSourceType(entity.getSwaggerSourceType());
        detail.setArtifacts(artifactRepository.findByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(this::toArtifactDto)
                .collect(Collectors.toList()));
        detail.setExternalOperations(externalOperationRepository.findByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(this::toExternalDto)
                .collect(Collectors.toList()));
        return detail;
    }

    public List<RunArtifactDto> getArtifacts(String runId) {
        if (!runHistoryRepository.existsById(runId)) {
            throw new NoSuchElementException("Run history not found: " + runId);
        }
        return artifactRepository.findByRunIdOrderByCreatedAtAsc(runId).stream()
                .map(this::toArtifactDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRun(String runId) {
        if (!runHistoryRepository.existsById(runId)) {
            throw new NoSuchElementException("Run history not found: " + runId);
        }
        artifactRepository.deleteByRunId(runId);
        externalOperationRepository.deleteByRunId(runId);
        runHistoryRepository.deleteById(runId);
    }

    private AgentRunHistoryEntity createOperationRun(String jiraStoryKey, String executionMode) {
        Instant now = Instant.now();
        AgentRunHistoryEntity entity = new AgentRunHistoryEntity();
        entity.setRunId(newRunId());
        entity.setJiraStoryKey(jiraStoryKey);
        entity.setExecutionMode(executionMode);
        entity.setStatus("RECORDED");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setTestCaseCount(0);
        entity.setGeneratedFileCount(0);
        return entity;
    }

    private void saveArtifact(String runId, String type, String name, String content) {
        RunArtifactEntity artifact = new RunArtifactEntity();
        artifact.setRunId(runId);
        artifact.setArtifactType(type);
        artifact.setName(name);
        artifact.setContent(content);
        artifact.setCreatedAt(Instant.now());
        artifactRepository.save(artifact);
    }

    private void saveExternalOperation(
            String runId,
            String type,
            String externalId,
            String status,
            String url,
            String details
    ) {
        ExternalOperationEntity operation = new ExternalOperationEntity();
        operation.setRunId(runId);
        operation.setOperationType(type);
        operation.setExternalId(externalId);
        operation.setStatus(status);
        operation.setUrl(url);
        operation.setDetailsJson(details);
        operation.setCreatedAt(Instant.now());
        externalOperationRepository.save(operation);
    }

    private RunHistorySummaryDto toSummary(AgentRunHistoryEntity entity) {
        RunHistorySummaryDto dto = new RunHistorySummaryDto();
        copySummary(entity, dto);
        return dto;
    }

    private void copySummary(AgentRunHistoryEntity entity, RunHistorySummaryDto dto) {
        dto.setRunId(entity.getRunId());
        dto.setJiraStoryKey(entity.getJiraStoryKey());
        dto.setStatus(entity.getStatus());
        dto.setExecutionMode(entity.getExecutionMode());
        dto.setTestGenerationMode(entity.getTestGenerationMode());
        dto.setFrameworkType(entity.getFrameworkType());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setTestCaseCount(entity.getTestCaseCount() != null ? entity.getTestCaseCount() : 0);
        dto.setGeneratedFileCount(entity.getGeneratedFileCount() != null ? entity.getGeneratedFileCount() : 0);
        dto.setFileWriteSummary(entity.getFileWriteSummary());
        dto.setTestExecutionStatus(entity.getTestExecutionStatus());
        dto.setGitPrUrl(entity.getGitPrUrl());
        dto.setJiraUpdateStatus(entity.getJiraUpdateStatus());
        dto.setWarnings(readStringList(entity.getWarningsJson()));
        dto.setErrors(readStringList(entity.getErrorsJson()));
    }

    private RunArtifactDto toArtifactDto(RunArtifactEntity entity) {
        RunArtifactDto dto = new RunArtifactDto();
        dto.setId(entity.getId());
        dto.setRunId(entity.getRunId());
        dto.setArtifactType(entity.getArtifactType());
        dto.setName(entity.getName());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private ExternalOperationDto toExternalDto(ExternalOperationEntity entity) {
        ExternalOperationDto dto = new ExternalOperationDto();
        dto.setId(entity.getId());
        dto.setRunId(entity.getRunId());
        dto.setOperationType(entity.getOperationType());
        dto.setExternalId(entity.getExternalId());
        dto.setStatus(entity.getStatus());
        dto.setUrl(entity.getUrl());
        dto.setDetailsJson(entity.getDetailsJson());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private String resolveSwaggerSourceType(AgentRequest request) {
        if (request.getSwaggerJson() != null && !request.getSwaggerJson().isBlank()) {
            return "JSON";
        }
        if (request.getSwaggerUrl() != null && !request.getSwaggerUrl().isBlank()) {
            return "URL";
        }
        return "NONE";
    }

    private String summaryText(FileWriteSummary summary) {
        if (summary == null) {
            return "";
        }
        return "total=" + summary.getTotal()
                + ", written=" + summary.getWritten()
                + ", blocked=" + summary.getBlocked()
                + ", errors=" + summary.getErrors();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of(json);
        }
    }

    private List<String> mergeLists(List<String> first, List<String> second) {
        if (first == null || first.isEmpty()) {
            return second != null ? second : List.of();
        }
        if (second == null || second.isEmpty()) {
            return first;
        }
        List<String> merged = new java.util.ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }
}
