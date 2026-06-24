package com.agentic.api.service;

import com.agentic.api.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AutomationGenerationService {

    private final OpenApiParserService openApiParserService;
    private final ContractTestMatrixService contractTestMatrixService;
    private final AiTestMatrixService aiTestMatrixService;
    private final AiBddGenerationService aiBddGenerationService;
    private final AiAutomationFileGenerationService aiAutomationFileGenerationService;

    public AutomationGenerationService(
            OpenApiParserService openApiParserService,
            ContractTestMatrixService contractTestMatrixService,
            AiTestMatrixService aiTestMatrixService,
            AiBddGenerationService aiBddGenerationService,
            AiAutomationFileGenerationService aiAutomationFileGenerationService
    ) {
        this.openApiParserService = openApiParserService;
        this.contractTestMatrixService = contractTestMatrixService;
        this.aiTestMatrixService = aiTestMatrixService;
        this.aiBddGenerationService = aiBddGenerationService;
        this.aiAutomationFileGenerationService = aiAutomationFileGenerationService;
    }

    public AutomationGenerationResponse generateAiBdd(AutomationGenerationRequest request) {
        ResolvedContext context = resolveContext(request);
        AiBddGenerationService.BddGenerationResult bddResult = aiBddGenerationService.generateBdd(
                context.agentRequest(),
                context.contract(),
                context.testCases()
        );

        AutomationGenerationResponse response = new AutomationGenerationResponse();
        response.setGeneratedBdd(bddResult.bdd());
        response.getWarnings().addAll(context.warnings());
        response.getWarnings().addAll(bddResult.warnings());
        response.getAssumptions().addAll(context.assumptions());
        response.getAssumptions().addAll(bddResult.assumptions());
        response.setSource(bddResult.fallbackUsed() ? "DETERMINISTIC" : "AI");
        response.setFallbackUsed(bddResult.fallbackUsed());
        return response;
    }

    public AutomationGenerationResponse generateAiFiles(AutomationGenerationRequest request) {
        ResolvedContext context = resolveContext(request);
        AiBddGenerationService.BddGenerationResult bddResult = aiBddGenerationService.generateBdd(
                context.agentRequest(),
                context.contract(),
                context.testCases()
        );

        AiAutomationFileGenerationService.FilesGenerationResult filesResult =
                aiAutomationFileGenerationService.generateFiles(
                        context.agentRequest(),
                        context.contract(),
                        context.testCases(),
                        bddResult.bdd()
                );

        boolean fallbackUsed = bddResult.fallbackUsed() || filesResult.fallbackUsed();
        String source = fallbackUsed ? "DETERMINISTIC" : "AI";

        AutomationGenerationResponse response = new AutomationGenerationResponse();
        response.setGeneratedBdd(bddResult.bdd());
        response.setGeneratedFiles(filesResult.files());
        response.getWarnings().addAll(context.warnings());
        response.getWarnings().addAll(bddResult.warnings());
        response.getWarnings().addAll(filesResult.warnings());
        response.getAssumptions().addAll(context.assumptions());
        response.getAssumptions().addAll(bddResult.assumptions());
        response.getAssumptions().addAll(filesResult.assumptions());
        response.setSource(source);
        response.setFallbackUsed(fallbackUsed);
        return response;
    }

    public AutomationGenerationResponse generateAiAutomationPackage(AutomationGenerationRequest request) {
        ResolvedContext context = resolveContext(request);

        AiBddGenerationService.BddGenerationResult bddResult = aiBddGenerationService.generateBdd(
                context.agentRequest(),
                context.contract(),
                context.testCases()
        );

        AiAutomationFileGenerationService.FilesGenerationResult filesResult =
                aiAutomationFileGenerationService.generateFiles(
                        context.agentRequest(),
                        context.contract(),
                        context.testCases(),
                        bddResult.bdd()
                );

        boolean fallbackUsed = bddResult.fallbackUsed() || filesResult.fallbackUsed();
        String source = fallbackUsed ? "DETERMINISTIC" : "AI";

        AutomationGenerationResponse response = new AutomationGenerationResponse();
        response.setGeneratedBdd(bddResult.bdd());
        response.setGeneratedFiles(filesResult.files());
        response.getWarnings().addAll(context.warnings());
        response.getWarnings().addAll(bddResult.warnings());
        response.getWarnings().addAll(filesResult.warnings());
        response.getAssumptions().addAll(context.assumptions());
        response.getAssumptions().addAll(bddResult.assumptions());
        response.getAssumptions().addAll(filesResult.assumptions());
        response.setSource(source);
        response.setFallbackUsed(fallbackUsed);
        return response;
    }

    private ResolvedContext resolveContext(AutomationGenerationRequest request) {
        AgentRequest agentRequest = request.getAgentRequest();
        List<String> warnings = new ArrayList<>();
        List<String> assumptions = new ArrayList<>();

        ApiContractDto contract = request.getApiContract();
        if (contract == null) {
            contract = openApiParserService.extractContract(agentRequest);
            warnings.add("API contract was re-extracted from Swagger because none was provided.");
        }

        List<TestCaseDto> testCases = request.getTestCases();
        if (testCases == null || testCases.isEmpty()) {
            TestMatrixResponse matrix = resolveTestMatrix(agentRequest, contract);
            testCases = matrix.getTestCases();
            warnings.add("Test cases were generated because none were provided.");
            warnings.addAll(matrix.getWarnings());
            assumptions.addAll(matrix.getAssumptions());
        }

        return new ResolvedContext(agentRequest, contract, testCases, warnings, assumptions);
    }

    private TestMatrixResponse resolveTestMatrix(AgentRequest agentRequest, ApiContractDto contract) {
        if ("ai-assisted".equalsIgnoreCase(agentRequest.getTestGenerationMode())) {
            return aiTestMatrixService.generateAiTestMatrix(agentRequest);
        }
        return contractTestMatrixService.generateFromContract(contract);
    }

    private record ResolvedContext(
            AgentRequest agentRequest,
            ApiContractDto contract,
            List<TestCaseDto> testCases,
            List<String> warnings,
            List<String> assumptions
    ) {
    }
}
