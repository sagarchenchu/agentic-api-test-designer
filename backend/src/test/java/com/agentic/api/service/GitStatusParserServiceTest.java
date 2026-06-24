package com.agentic.api.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitStatusParserServiceTest {

    private final GitStatusParserService parser = new GitStatusParserService();

    @Test
    void parsesStatusShortOutput() {
        String output = " M src/test/resources/features/payment/a.feature\n"
                + "?? src/test/java/steps/PaymentSteps.java\n";

        Map<String, String> status = parser.parseStatusShort(output);

        assertEquals("M", status.get("src/test/resources/features/payment/a.feature"));
        assertEquals("??", status.get("src/test/java/steps/PaymentSteps.java"));
    }

    @Test
    void findsUnrelatedChanges() {
        Map<String, String> status = parser.parseStatusShort(
                " M pom.xml\n M src/test/resources/features/payment/a.feature\n"
        );

        List<String> unrelated = parser.findUnrelatedChanges(
                status,
                List.of("src/test/resources/features/payment/a.feature")
        );

        assertEquals(1, unrelated.size());
        assertTrue(unrelated.get(0).contains("pom.xml"));
    }

    @Test
    void matchingChangedFilesReturnsOnlyListedFilesInStatus() {
        Map<String, String> status = parser.parseStatusShort(
                " M src/test/resources/features/payment/a.feature\n M pom.xml\n"
        );

        List<String> changed = parser.matchingChangedFiles(
                status,
                List.of(
                        "src/test/resources/features/payment/a.feature",
                        "src/test/java/steps/PaymentSteps.java"
                )
        );

        assertEquals(List.of("src/test/resources/features/payment/a.feature"), changed);
    }

    @Test
    void parsesPrUrlFromGhOutput() {
        String url = parser.parsePrUrl(List.of(
                "Creating pull request for feature/PAY-1234 into main in org/repo",
                "https://github.com/org/repo/pull/123"
        ));
        assertEquals("https://github.com/org/repo/pull/123", url);
    }
}
