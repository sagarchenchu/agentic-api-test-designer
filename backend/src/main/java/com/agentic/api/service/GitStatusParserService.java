package com.agentic.api.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GitStatusParserService {

    public Map<String, String> parseStatusShort(String output) {
        Map<String, String> statusByPath = new LinkedHashMap<>();
        if (output == null || output.isBlank()) {
            return statusByPath;
        }
        for (String line : output.split("\n")) {
            if (line.length() < 4) {
                continue;
            }
            String status = line.substring(0, 2).trim();
            String path = line.substring(3).trim();
            if (path.contains(" -> ")) {
                path = path.substring(path.indexOf(" -> ") + 4).trim();
            }
            if (!path.isBlank()) {
                statusByPath.put(normalizePath(path), status.isBlank() ? "?" : status);
            }
        }
        return statusByPath;
    }

    public List<String> findUnrelatedChanges(Map<String, String> statusByPath, List<String> filesToCommit) {
        Set<String> allowed = filesToCommit.stream().map(this::normalizePath).collect(Collectors.toSet());
        List<String> unrelated = new ArrayList<>();
        for (Map.Entry<String, String> entry : statusByPath.entrySet()) {
            if (!allowed.contains(entry.getKey())) {
                unrelated.add(entry.getKey() + " (" + entry.getValue() + ")");
            }
        }
        return unrelated;
    }

    public List<String> matchingChangedFiles(Map<String, String> statusByPath, List<String> filesToCommit) {
        Set<String> allowed = filesToCommit.stream().map(this::normalizePath).collect(Collectors.toSet());
        List<String> changed = new ArrayList<>();
        for (String file : allowed) {
            if (statusByPath.containsKey(file)) {
                changed.add(file);
            }
        }
        return changed;
    }

    public String parsePrUrl(List<String> outputLines) {
        for (int i = outputLines.size() - 1; i >= 0; i--) {
            String line = outputLines.get(i).trim();
            if (line.startsWith("http://") || line.startsWith("https://")) {
                return line;
            }
        }
        return null;
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/');
    }
}
