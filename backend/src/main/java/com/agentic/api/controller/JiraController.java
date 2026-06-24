package com.agentic.api.controller;

import com.agentic.api.model.*;
import com.agentic.api.service.JiraStoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/jira")
public class JiraController {

    private final JiraStoryService jiraStoryService;

    public JiraController(JiraStoryService jiraStoryService) {
        this.jiraStoryService = jiraStoryService;
    }

    @GetMapping("/config/status")
    public JiraConfigStatusResponse getConfigStatus() {
        return jiraStoryService.getConfigStatus();
    }

    @PostMapping("/fetch-story")
    public JiraStoryResponse fetchStory(@Valid @RequestBody JiraStoryRequest request) {
        return jiraStoryService.fetchStory(request);
    }

    @PostMapping("/post-summary")
    public JiraOperationResponse postSummary(@Valid @RequestBody JiraPostSummaryRequest request) {
        return jiraStoryService.postSummary(request);
    }

    @PostMapping("/link-pr")
    public JiraOperationResponse linkPr(@Valid @RequestBody JiraLinkPrRequest request) {
        return jiraStoryService.linkPr(request);
    }
}
