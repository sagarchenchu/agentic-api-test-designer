package com.agentic.api.controller;

import com.agentic.api.model.RunArtifactDto;
import com.agentic.api.model.RunHistoryDetailDto;
import com.agentic.api.model.RunHistorySummaryDto;
import com.agentic.api.service.RunHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/agent/history")
public class RunHistoryController {

    private final RunHistoryService runHistoryService;

    public RunHistoryController(RunHistoryService runHistoryService) {
        this.runHistoryService = runHistoryService;
    }

    @GetMapping("/runs")
    public List<RunHistorySummaryDto> listRuns() {
        return runHistoryService.listRuns();
    }

    @GetMapping("/runs/{runId}")
    public RunHistoryDetailDto getRun(@PathVariable String runId) {
        return runHistoryService.getRun(runId);
    }

    @DeleteMapping("/runs/{runId}")
    public Map<String, String> deleteRun(@PathVariable String runId) {
        runHistoryService.deleteRun(runId);
        return Map.of("status", "deleted", "runId", runId);
    }

    @GetMapping("/runs/{runId}/artifacts")
    public List<RunArtifactDto> getArtifacts(@PathVariable String runId) {
        return runHistoryService.getArtifacts(runId);
    }
}
