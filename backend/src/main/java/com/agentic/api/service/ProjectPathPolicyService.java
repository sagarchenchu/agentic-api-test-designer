package com.agentic.api.service;

import com.agentic.api.config.AgentProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectPathPolicyService {

    private final AgentProperties agentProperties;

    public ProjectPathPolicyService(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public List<String> validateProjectPath(String projectPath) {
        List<String> errors = new ArrayList<>();
        if (projectPath == null || projectPath.isBlank()) {
            errors.add("projectPath is required");
            return errors;
        }

        Path path = Paths.get(projectPath.trim()).toAbsolutePath().normalize();
        if (agentProperties.isAllowAnyLocalPath()) {
            return errors;
        }

        List<Path> allowedRoots = parseAllowedRoots();
        if (allowedRoots.isEmpty()) {
            errors.add("projectPath is not allowed: no allowed project roots configured");
            return errors;
        }

        boolean allowed = allowedRoots.stream().anyMatch(root -> path.startsWith(root));
        if (!allowed) {
            errors.add("projectPath is outside allowed project roots: " + path);
        }
        return errors;
    }

    private List<Path> parseAllowedRoots() {
        List<Path> roots = new ArrayList<>();
        String configured = agentProperties.getAllowedProjectRoots();
        if (configured == null || configured.isBlank()) {
            return roots;
        }
        for (String part : configured.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                roots.add(Paths.get(trimmed).toAbsolutePath().normalize());
            }
        }
        return roots;
    }
}
