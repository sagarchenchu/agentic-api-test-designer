package com.agentic.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JiraKeyValidatorTest {

    private final JiraKeyValidator validator = new JiraKeyValidator();

    @Test
    void acceptsValidKeysAndNormalizesCase() {
        assertEquals("PAY-1234", validator.normalizeAndValidate("pay-1234"));
        assertEquals("ABC9-22", validator.normalizeAndValidate("ABC9-22"));
    }

    @Test
    void rejectsInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> validator.normalizeAndValidate("pay_1234"));
        assertThrows(IllegalArgumentException.class, () -> validator.normalizeAndValidate("PAY; rm -rf"));
        assertThrows(IllegalArgumentException.class, () -> validator.normalizeAndValidate(""));
    }
}
