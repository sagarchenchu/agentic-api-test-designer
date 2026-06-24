package com.agentic.api.support;

import com.agentic.api.config.AgentProperties;
import com.agentic.api.config.JiraProperties;
import com.agentic.api.config.OpenAiProperties;
import com.agentic.api.config.SecurityProperties;
import com.agentic.api.service.OperationConfirmationService;
import com.agentic.api.service.ProjectPathPolicyService;
import com.agentic.api.service.RunHistoryService;
import com.agentic.api.service.SecretMaskingService;

import static org.mockito.Mockito.mock;

public final class TestSupport {

    private TestSupport() {
    }

    public static ProjectPathPolicyService permissivePathPolicy() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowAnyLocalPath(true);
        return new ProjectPathPolicyService(properties);
    }

    public static SecretMaskingService secretMaskingService() {
        return new SecretMaskingService(new OpenAiProperties(), new JiraProperties(), new SecurityProperties());
    }

    public static OperationConfirmationService operationConfirmationService() {
        return new OperationConfirmationService(new SecurityProperties());
    }

    public static RunHistoryService mockRunHistoryService() {
        return mock(RunHistoryService.class);
    }
}
