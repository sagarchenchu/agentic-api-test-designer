package com.agentic.api.controller;

import com.agentic.api.model.*;
import com.agentic.api.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/extract-contract")
    public ApiContractDto extractContract(@Valid @RequestBody AgentRequest request) {
        return agentService.extractContract(request);
    }

    @PostMapping("/generate-test-matrix")
    public TestMatrixResponse generateTestMatrix(@Valid @RequestBody AgentRequest request) {
        return agentService.generateTestMatrix(request);
    }

    @PostMapping("/generate-ai-test-matrix")
    public TestMatrixResponse generateAiTestMatrix(@Valid @RequestBody AgentRequest request) {
        return agentService.generateAiTestMatrix(request);
    }

    @PostMapping("/generate-bdd")
    public GeneratedBddDto generateBdd(@Valid @RequestBody AgentRequest request) {
        return agentService.generateBdd(request);
    }

    @PostMapping("/generate-files")
    public GeneratedFilesDto generateFiles(@Valid @RequestBody AgentRequest request) {
        return agentService.generateFiles(request);
    }

    @PostMapping("/generate-ai-bdd")
    public AutomationGenerationResponse generateAiBdd(
            @Valid @RequestBody AutomationGenerationRequest request
    ) {
        return agentService.generateAiBdd(request);
    }

    @PostMapping("/generate-ai-files")
    public AutomationGenerationResponse generateAiFiles(
            @Valid @RequestBody AutomationGenerationRequest request
    ) {
        return agentService.generateAiFiles(request);
    }

    @PostMapping("/generate-ai-automation-package")
    public AutomationGenerationResponse generateAiAutomationPackage(
            @Valid @RequestBody AutomationGenerationRequest request
    ) {
        return agentService.generateAiAutomationPackage(request);
    }

    @PostMapping("/run")
    public AgentRunResponse runAgent(@Valid @RequestBody AgentRequest request) {
        return agentService.runAgent(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/runs/{runId}")
    public AgentRunResponse getRun(@PathVariable String runId) {
        return agentService.getRun(runId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
