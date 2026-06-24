package com.agentic.api.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitCommandBuilderTest {

    private final GitCommandBuilder builder = new GitCommandBuilder();

    @Test
    void statusUsesArgList() {
        assertEquals(List.of("git", "status", "--short"), builder.statusShort());
    }

    @Test
    void checkoutNewBranchUsesArgList() {
        assertEquals(
                List.of("git", "checkout", "-b", "feature/PAY-1234-api-tests"),
                builder.checkoutNewBranch("feature/PAY-1234-api-tests")
        );
    }

    @Test
    void addUsesArgListForEachFile() {
        List<String> command = builder.add(List.of(
                "src/test/resources/features/payment/a.feature",
                "src/test/java/steps/PaymentSteps.java"
        ));
        assertEquals("git", command.get(0));
        assertEquals("add", command.get(1));
        assertEquals("src/test/resources/features/payment/a.feature", command.get(2));
        assertEquals("src/test/java/steps/PaymentSteps.java", command.get(3));
        assertFalse(command.contains("."));
    }

    @Test
    void ghPrCreateUsesArgList() {
        List<String> command = builder.ghPrCreate(
                "main",
                "feature/PAY-1234-api-tests",
                "PAY-1234 Add API automation tests",
                "Generated API tests."
        );
        assertEquals(List.of(
                "gh", "pr", "create",
                "--base", "main",
                "--head", "feature/PAY-1234-api-tests",
                "--title", "PAY-1234 Add API automation tests",
                "--body", "Generated API tests."
        ), command);
    }
}
