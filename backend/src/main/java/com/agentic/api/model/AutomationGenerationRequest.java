package com.agentic.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AutomationGenerationRequest {

    @NotNull(message = "agentRequest is required")
    @Valid
    private AgentRequest agentRequest;

    private ApiContractDto apiContract;

    private List<TestCaseDto> testCases = new ArrayList<>();

    public AgentRequest getAgentRequest() {
        return agentRequest;
    }

    public void setAgentRequest(AgentRequest agentRequest) {
        this.agentRequest = agentRequest;
    }

    public ApiContractDto getApiContract() {
        return apiContract;
    }

    public void setApiContract(ApiContractDto apiContract) {
        this.apiContract = apiContract;
    }

    public List<TestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseDto> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }
}
