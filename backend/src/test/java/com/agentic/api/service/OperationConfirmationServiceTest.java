package com.agentic.api.service;

import com.agentic.api.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationConfirmationServiceTest {

    @Test
    void skipsConfirmationWhenSecurityDisabled() {
        SecurityProperties properties = new SecurityProperties();
        OperationConfirmationService service = new OperationConfirmationService(properties);
        assertDoesNotThrow(() -> service.requireConfirmation(null));
    }

    @Test
    void requiresConfirmationWhenSecurityEnabled() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEnabled(true);
        OperationConfirmationService service = new OperationConfirmationService(properties);

        assertThrows(IllegalArgumentException.class, () -> service.requireConfirmation(null));
        assertDoesNotThrow(() -> service.requireConfirmation(OperationConfirmationService.REQUIRED_CONFIRMATION));
    }
}
