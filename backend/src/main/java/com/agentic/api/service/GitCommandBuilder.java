package com.agentic.api.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GitCommandBuilder {

    public List<String> revParseTopLevel() {
        return List.of("git", "rev-parse", "--show-toplevel");
    }

    public List<String> statusShort() {
        return List.of("git", "status", "--short");
    }

    public List<String> branchShowCurrent() {
        return List.of("git", "branch", "--show-current");
    }

    public List<String> checkout(String branch) {
        return List.of("git", "checkout", branch);
    }

    public List<String> checkoutNewBranch(String branch) {
        return List.of("git", "checkout", "-b", branch);
    }

    public List<String> pull(String remote, String branch) {
        return List.of("git", "pull", remote, branch);
    }

    public List<String> add(List<String> files) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("add");
        command.addAll(files);
        return command;
    }

    public List<String> commit(String message) {
        return List.of("git", "commit", "-m", message);
    }

    public List<String> revParseHead() {
        return List.of("git", "rev-parse", "HEAD");
    }

    public List<String> push(String remote, String branch) {
        return List.of("git", "push", remote, branch);
    }

    public List<String> ghVersion() {
        return List.of("gh", "--version");
    }

    public List<String> ghPrCreate(String baseBranch, String headBranch, String title, String body) {
        return List.of(
                "gh", "pr", "create",
                "--base", baseBranch,
                "--head", headBranch,
                "--title", title,
                "--body", body
        );
    }

    public String formatCommand(List<String> command) {
        return String.join(" ", command);
    }

    public List<String> formatCommands(List<List<String>> commands) {
        return commands.stream().map(this::formatCommand).collect(Collectors.toList());
    }
}
