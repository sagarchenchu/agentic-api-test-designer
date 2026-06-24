package com.agentic.api.service;

import com.agentic.api.model.TestExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MavenCommandBuilderTest {

    private MavenCommandBuilder mavenCommandBuilder;

    @BeforeEach
    void setUp() {
        mavenCommandBuilder = new MavenCommandBuilder();
    }

    @Test
    void buildsSafeMavenCommandWithTagAndProfile() {
        MavenCommandBuilder.MavenCommandBuildResult result =
                mavenCommandBuilder.build(validRequest());

        assertTrue(result.isValid());
        assertEquals(
                "mvn clean verify -Dcucumber.filter.tags=@PAY-1234 -Pqa -Denv=QA",
                result.getCommandString()
        );
        assertEquals(
                java.util.List.of(
                        "mvn", "clean", "verify",
                        "-Dcucumber.filter.tags=@PAY-1234",
                        "-Pqa",
                        "-Denv=QA"
                ),
                result.getArguments()
        );
    }

    @Test
    void rejectsUnsafeProfile() {
        TestExecutionRequest request = validRequest();
        request.setProfile("qa; rm -rf /");

        MavenCommandBuilder.MavenCommandBuildResult result = mavenCommandBuilder.build(request);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("profile")));
    }

    @Test
    void rejectsUnsafeMavenCommand() {
        TestExecutionRequest request = validRequest();
        request.setMavenCommand("mvn clean verify && rm -rf /");

        MavenCommandBuilder.MavenCommandBuildResult result = mavenCommandBuilder.build(request);

        assertFalse(result.isValid());
    }

    @Test
    void rejectsInvalidTestTag() {
        TestExecutionRequest request = validRequest();
        request.setTestTag("@PAY 1234");

        MavenCommandBuilder.MavenCommandBuildResult result = mavenCommandBuilder.build(request);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("testTag")));
    }

    private TestExecutionRequest validRequest() {
        TestExecutionRequest request = new TestExecutionRequest();
        request.setProjectPath("/tmp/project");
        request.setMavenCommand("mvn clean verify");
        request.setTestTag("@PAY-1234");
        request.setProfile("qa");
        request.setEnvironment("QA");
        return request;
    }
}
