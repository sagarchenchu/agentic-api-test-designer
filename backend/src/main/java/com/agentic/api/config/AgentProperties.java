package com.agentic.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String allowedProjectRoots = "";
    private boolean allowAnyLocalPath = true;

    public String getAllowedProjectRoots() {
        return allowedProjectRoots;
    }

    public void setAllowedProjectRoots(String allowedProjectRoots) {
        this.allowedProjectRoots = allowedProjectRoots;
    }

    public boolean isAllowAnyLocalPath() {
        return allowAnyLocalPath;
    }

    public void setAllowAnyLocalPath(boolean allowAnyLocalPath) {
        this.allowAnyLocalPath = allowAnyLocalPath;
    }
}
