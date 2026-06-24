package com.agentic.api.service;

import com.agentic.api.config.SecurityProperties;
import org.springframework.stereotype.Service;

@Service
public class OperationConfirmationService {

    public static final String REQUIRED_CONFIRMATION = "I_UNDERSTAND";

    private final SecurityProperties securityProperties;

    public OperationConfirmationService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void requireConfirmation(String confirmation) {
        if (!securityProperties.isEnabled()) {
            return;
        }
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException(
                    "confirmation is required and must be \"" + REQUIRED_CONFIRMATION + "\" when security is enabled"
            );
        }
    }
}
