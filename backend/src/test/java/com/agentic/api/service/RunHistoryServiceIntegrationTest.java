package com.agentic.api.service;

import com.agentic.api.entity.AgentRunHistoryEntity;
import com.agentic.api.model.AgentRequest;
import com.agentic.api.model.AgentRunResponse;
import com.agentic.api.model.TestCaseDto;
import com.agentic.api.repository.AgentRunHistoryRepository;
import com.agentic.api.repository.ExternalOperationRepository;
import com.agentic.api.repository.RunArtifactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RunHistoryServiceIntegrationTest {

    @Autowired
    private RunHistoryService runHistoryService;

    @Autowired
    private AgentRunHistoryRepository runHistoryRepository;

    @Autowired
    private RunArtifactRepository artifactRepository;

    @Autowired
    private ExternalOperationRepository externalOperationRepository;

    @Test
    void savesListsGetsAndDeletesRunHistory() {
        AgentRequest request = new AgentRequest();
        request.setJiraStoryKey("PAY-1234");
        request.setExecutionMode("generate-execute");
        request.setTestGenerationMode("deterministic");
        request.setFrameworkType("restassured-cucumber-serenity");
        request.setSwaggerUrl("https://example.com/swagger.json");

        AgentRunResponse response = new AgentRunResponse();
        response.setRunId("run-history-test-1");
        response.setStatus("completed");
        response.setTestCases(List.of(new TestCaseDto()));

        runHistoryService.recordAgentRun(request, response);

        assertEquals(1, runHistoryService.listRuns().stream()
                .filter(run -> "run-history-test-1".equals(run.getRunId()))
                .count());

        var detail = runHistoryService.getRun("run-history-test-1");
        assertEquals("PAY-1234", detail.getJiraStoryKey());
        assertEquals("completed", detail.getStatus());

        runHistoryService.deleteRun("run-history-test-1");
        assertFalse(runHistoryRepository.existsById("run-history-test-1"));
        assertTrue(artifactRepository.findByRunIdOrderByCreatedAtAsc("run-history-test-1").isEmpty());
        assertTrue(externalOperationRepository.findByRunIdOrderByCreatedAtAsc("run-history-test-1").isEmpty());
    }
}
