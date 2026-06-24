package com.agentic.api.service;

import com.agentic.api.model.*;

import java.util.List;

public interface AgentService {

    List<TestCaseDto> generateTestMatrix(AgentRequest request);

    GeneratedBddDto generateBdd(AgentRequest request);

    GeneratedFilesDto generateFiles(AgentRequest request);

    AgentRunResponse runAgent(AgentRequest request);

    AgentRunResponse getRun(String runId);
}
