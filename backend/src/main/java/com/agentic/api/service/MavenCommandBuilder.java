package com.agentic.api.service;

import com.agentic.api.model.TestExecutionRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class MavenCommandBuilder {

    private static final Set<String> ALLOWED_GOALS = Set.of(
            "mvn clean test",
            "mvn clean verify",
            "mvn test",
            "mvn verify"
    );

    private static final Pattern UNSAFE_PATTERN = Pattern.compile(
            "(?i)(;|&&|\\|\\||[|><`$]|\\.\\./|powershell|cmd\\.exe|\\bcmd\\b|\\bbash\\b|\\bsh\\b|curl|wget|\\brm\\b|\\bdel\\b|format)"
    );

    private static final Pattern TEST_TAG_PATTERN = Pattern.compile("^@[A-Za-z0-9][A-Za-z0-9._-]*$");
    private static final Pattern PROFILE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");
    private static final Pattern ENV_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]*$");

    public MavenCommandBuildResult build(TestExecutionRequest request) {
        MavenCommandBuildResult result = new MavenCommandBuildResult();
        List<String> errors = result.getErrors();

        if (!"MAVEN".equalsIgnoreCase(safe(request.getCommandType()))) {
            errors.add("Only MAVEN commandType is supported");
            return result;
        }

        String mavenCommand = normalizeSpaces(safe(request.getMavenCommand()));
        if (!ALLOWED_GOALS.contains(mavenCommand)) {
            errors.add("mavenCommand must be one of: mvn test, mvn verify, mvn clean test, mvn clean verify");
            return result;
        }

        if (containsUnsafe(mavenCommand)) {
            errors.add("mavenCommand contains unsafe characters or commands");
            return result;
        }

        String testTag = safe(request.getTestTag());
        if (!testTag.isBlank()) {
            if (containsUnsafe(testTag) || !TEST_TAG_PATTERN.matcher(testTag).matches()) {
                errors.add("testTag must be a safe Cucumber tag like @PAY-1234");
                return result;
            }
        }

        String profile = safe(request.getProfile());
        if (!profile.isBlank()) {
            if (containsUnsafe(profile) || !PROFILE_PATTERN.matcher(profile).matches()) {
                errors.add("profile must contain only letters, numbers, hyphens, or underscores");
                return result;
            }
        }

        String environment = safe(request.getEnvironment());
        if (!environment.isBlank()) {
            if (containsUnsafe(environment) || !ENV_PATTERN.matcher(environment).matches()) {
                errors.add("environment must contain only letters, numbers, hyphens, or underscores");
                return result;
            }
        }

        List<String> args = new ArrayList<>(List.of(mavenCommand.split(" ")));
        if (!testTag.isBlank()) {
            args.add("-Dcucumber.filter.tags=" + testTag);
        }
        if (!profile.isBlank()) {
            args.add("-P" + profile);
        }
        if (!environment.isBlank()) {
            args.add("-Denv=" + environment);
        }

        result.setArguments(args);
        result.setCommandString(String.join(" ", args));
        return result;
    }

    boolean containsUnsafe(String value) {
        return UNSAFE_PATTERN.matcher(value).find();
    }

    private String normalizeSpaces(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class MavenCommandBuildResult {
        private List<String> arguments = new ArrayList<>();
        private String commandString;
        private final List<String> errors = new ArrayList<>();

        public List<String> getArguments() {
            return arguments;
        }

        public void setArguments(List<String> arguments) {
            this.arguments = arguments;
        }

        public String getCommandString() {
            return commandString;
        }

        public void setCommandString(String commandString) {
            this.commandString = commandString;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isValid() {
            return errors.isEmpty() && !arguments.isEmpty();
        }
    }
}
