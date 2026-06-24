package com.agentic.api.service;

import com.agentic.api.config.AgentProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectPathPolicyServiceTest {

    @Test
    void allowsAnyPathWhenPermissive() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowAnyLocalPath(true);
        ProjectPathPolicyService service = new ProjectPathPolicyService(properties);

        assertTrue(service.validateProjectPath("/tmp/project").isEmpty());
    }

    @Test
    void blocksPathOutsideAllowedRoots() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowAnyLocalPath(false);
        properties.setAllowedProjectRoots("/allowed/root");
        ProjectPathPolicyService service = new ProjectPathPolicyService(properties);

        assertFalse(service.validateProjectPath("/other/project").isEmpty());
    }
}
