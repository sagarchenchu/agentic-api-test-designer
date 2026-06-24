package com.agentic.api.service;

import com.agentic.api.model.*;

public interface AgentService {

    TestMatrixResponse generateTestMatrix(AgentRequest request);

    TestMatrixResponse generateAiTestMatrix(AgentRequest request);

    GeneratedBddDto generateBdd(AgentRequest request);

    GeneratedFilesDto generateFiles(AgentRequest request);

    AgentRunResponse runAgent(AgentRequest request);

    AgentRunResponse getRun(String runId);

    ApiContractDto extractContract(AgentRequest request);

    AutomationGenerationResponse generateAiBdd(AutomationGenerationRequest request);

    AutomationGenerationResponse generateAiFiles(AutomationGenerationRequest request);

    AutomationGenerationResponse generateAiAutomationPackage(AutomationGenerationRequest request);

    FileWriteResponse previewFileWrite(FileWriteRequest request);

    FileWriteResponse writeGeneratedFiles(FileWriteRequest request);
}
