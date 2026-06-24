package com.agentic.api.service;

import com.agentic.api.model.FailedScenarioDto;
import com.agentic.api.model.TestExecutionSummary;
import com.agentic.api.model.TestReportPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TestReportParserServiceTest {

    @TempDir
    Path tempDir;

    private TestReportParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new TestReportParserService(new ObjectMapper());
    }

    @Test
    void parsesSurefireXml() throws Exception {
        Path reports = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(reports);
        Files.writeString(
                reports.resolve("TEST-PaymentApiTest.xml"),
                new ClassPathResource("sample-surefire.xml").getContentAsString(StandardCharsets.UTF_8)
        );

        TestExecutionSummary summary = parserService.parseSurefireFailsafe(tempDir);

        assertEquals(12, summary.getTotal());
        assertEquals(1, summary.getFailed());
        assertEquals(2, summary.getSkipped());
        assertEquals(9, summary.getPassed());
    }

    @Test
    void parsesCucumberFailedScenario() throws Exception {
        Path reports = tempDir.resolve("target/cucumber-reports");
        Files.createDirectories(reports);
        Files.writeString(
                reports.resolve("cucumber.json"),
                new ClassPathResource("sample-cucumber.json").getContentAsString(StandardCharsets.UTF_8)
        );

        var failures = parserService.parseCucumberFailures(tempDir);

        assertEquals(1, failures.size());
        FailedScenarioDto failed = failures.get(0);
        assertEquals("Create payment API", failed.getFeature());
        assertEquals("Missing required accountId", failed.getScenario());
        assertTrue(failed.getErrorMessage().contains("Expected status 400"));
    }

    @Test
    void detectsSerenityReportPath() throws Exception {
        Path serenity = tempDir.resolve("target/site/serenity");
        Files.createDirectories(serenity);
        Files.writeString(serenity.resolve("index.html"), "<html></html>");

        TestReportPaths paths = parserService.detectReportPaths(tempDir);

        assertEquals("target/site/serenity/index.html", paths.getSerenity());
    }
}
