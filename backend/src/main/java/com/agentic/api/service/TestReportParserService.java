package com.agentic.api.service;

import com.agentic.api.model.FailedScenarioDto;
import com.agentic.api.model.TestExecutionSummary;
import com.agentic.api.model.TestReportPaths;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TestReportParserService {

    private final ObjectMapper objectMapper;

    public TestReportParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TestExecutionSummary parseSurefireFailsafe(Path projectRoot) {
        TestExecutionSummary summary = new TestExecutionSummary();
        parseXmlDirectory(projectRoot.resolve("target/surefire-reports"), summary);
        parseXmlDirectory(projectRoot.resolve("target/failsafe-reports"), summary);
        summary.setPassed(Math.max(0,
                summary.getTotal() - summary.getFailed() - summary.getErrors() - summary.getSkipped()));
        return summary;
    }

    public List<FailedScenarioDto> parseCucumberFailures(Path projectRoot) {
        Path primary = projectRoot.resolve("target/cucumber-reports/cucumber.json");
        Path fallback = projectRoot.resolve("target/cucumber.json");
        if (Files.exists(primary)) {
            return parseCucumberFile(primary);
        }
        if (Files.exists(fallback)) {
            return parseCucumberFile(fallback);
        }
        return List.of();
    }

    public TestReportPaths detectReportPaths(Path projectRoot) {
        TestReportPaths paths = new TestReportPaths();
        Path surefire = projectRoot.resolve("target/surefire-reports");
        Path failsafe = projectRoot.resolve("target/failsafe-reports");
        Path serenity = projectRoot.resolve("target/site/serenity/index.html");
        Path cucumber = projectRoot.resolve("target/cucumber-reports/cucumber.json");
        Path cucumberFallback = projectRoot.resolve("target/cucumber.json");

        if (Files.isDirectory(surefire)) {
            paths.setSurefire(relativePath(projectRoot, surefire));
        }
        if (Files.isDirectory(failsafe)) {
            paths.setFailsafe(relativePath(projectRoot, failsafe));
        }
        if (Files.exists(serenity)) {
            paths.setSerenity(relativePath(projectRoot, serenity));
        }
        if (Files.exists(cucumber)) {
            paths.setCucumberJson(relativePath(projectRoot, cucumber));
        } else if (Files.exists(cucumberFallback)) {
            paths.setCucumberJson(relativePath(projectRoot, cucumberFallback));
        }
        return paths;
    }

    private void parseXmlDirectory(Path directory, TestExecutionSummary summary) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .forEach(path -> parseXmlFile(path, summary));
        } catch (Exception ignored) {
            // Ignore unreadable report directories in Phase 7
        }
    }

    void parseXmlFile(Path xmlFile, TestExecutionSummary summary) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(xmlFile.toFile());
            NodeList suites = document.getElementsByTagName("testsuite");
            for (int i = 0; i < suites.getLength(); i++) {
                Element suite = (Element) suites.item(i);
                summary.setTotal(summary.getTotal() + intAttr(suite, "tests"));
                summary.setFailed(summary.getFailed() + intAttr(suite, "failures"));
                summary.setErrors(summary.getErrors() + intAttr(suite, "errors"));
                summary.setSkipped(summary.getSkipped() + intAttr(suite, "skipped"));
            }
        } catch (Exception ignored) {
            // Ignore malformed XML files
        }
    }

    List<FailedScenarioDto> parseCucumberFile(Path cucumberJson) {
        List<FailedScenarioDto> failures = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(cucumberJson.toFile());
            if (!root.isArray()) {
                return failures;
            }
            for (JsonNode featureNode : root) {
                String featureName = featureNode.path("name").asText("");
                JsonNode elements = featureNode.path("elements");
                if (!elements.isArray()) {
                    continue;
                }
                for (JsonNode element : elements) {
                    if (!"scenario".equalsIgnoreCase(element.path("type").asText())) {
                        continue;
                    }
                    if (!hasFailedStep(element)) {
                        continue;
                    }
                    FailedScenarioDto failed = new FailedScenarioDto();
                    failed.setFeature(featureName);
                    failed.setScenario(element.path("name").asText("Unnamed scenario"));
                    failed.setErrorMessage(extractScenarioError(element));
                    failures.add(failed);
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed cucumber JSON
        }
        return failures;
    }

    private boolean hasFailedStep(JsonNode scenario) {
        JsonNode steps = scenario.path("steps");
        if (!steps.isArray()) {
            return false;
        }
        for (JsonNode step : steps) {
            JsonNode result = step.path("result");
            String status = result.path("status").asText("");
            if ("failed".equalsIgnoreCase(status)) {
                return true;
            }
        }
        return false;
    }

    private String extractScenarioError(JsonNode scenario) {
        JsonNode steps = scenario.path("steps");
        if (!steps.isArray()) {
            return "";
        }
        for (JsonNode step : steps) {
            JsonNode result = step.path("result");
            if ("failed".equalsIgnoreCase(result.path("status").asText())) {
                return result.path("error_message").asText("");
            }
        }
        return "";
    }

    private int intAttr(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String relativePath(Path projectRoot, Path target) {
        return projectRoot.relativize(target).toString().replace('\\', '/');
    }
}
