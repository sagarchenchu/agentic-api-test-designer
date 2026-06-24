package com.agentic.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JiraAdfTextExtractor {

    private static final Pattern AC_SECTION = Pattern.compile(
            "(?im)^(?:acceptance criteria|ac)\\s*:?\\s*$"
    );
    private static final Pattern BULLET_LINE = Pattern.compile(
            "^(?:[-*•]|\\d+[.)])\\s+(.+)$"
    );

    private final ObjectMapper objectMapper;

    public JiraAdfTextExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String adfToPlainText(JsonNode adfNode) {
        if (adfNode == null || adfNode.isNull() || adfNode.isMissingNode()) {
            return "";
        }
        if (adfNode.isTextual()) {
            return adfNode.asText();
        }
        StringBuilder sb = new StringBuilder();
        appendNodeText(adfNode, sb);
        return sb.toString().replaceAll("\n{3,}", "\n\n").trim();
    }

    public String adfToPlainText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return adfToPlainText(node);
        } catch (Exception ex) {
            return raw.trim();
        }
    }

    public List<String> extractAcceptanceCriteria(String plainText) {
        List<String> criteria = new ArrayList<>();
        if (plainText == null || plainText.isBlank()) {
            return criteria;
        }

        String[] lines = plainText.split("\\R");
        boolean inSection = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inSection && !criteria.isEmpty()) {
                    break;
                }
                continue;
            }

            if (!inSection) {
                if (AC_SECTION.matcher(trimmed).matches()) {
                    inSection = true;
                }
                continue;
            }

            Matcher bullet = BULLET_LINE.matcher(trimmed);
            if (bullet.matches()) {
                criteria.add(bullet.group(1).trim());
            } else if (!criteria.isEmpty()) {
                break;
            }
        }

        return criteria;
    }

    private void appendNodeText(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            sb.append(node.asText());
            return;
        }

        String type = node.path("type").asText("");
        if ("text".equals(type)) {
            sb.append(node.path("text").asText(""));
            return;
        }
        if ("hardBreak".equals(type)) {
            sb.append('\n');
            return;
        }
        if ("bulletList".equals(type)) {
            appendListItems(node.path("content"), sb, "- ");
            return;
        }
        if ("orderedList".equals(type)) {
            appendOrderedListItems(node.path("content"), sb);
            return;
        }
        if ("paragraph".equals(type) || "heading".equals(type) || "listItem".equals(type)) {
            appendChildren(node.path("content"), sb);
            sb.append('\n');
            return;
        }
        if ("blockquote".equals(type)) {
            appendChildren(node.path("content"), sb);
            return;
        }
        if ("doc".equals(type) || "mediaSingle".equals(type) || "panel".equals(type)) {
            appendChildren(node.path("content"), sb);
            return;
        }

        appendChildren(node.path("content"), sb);
    }

    private void appendListItems(JsonNode items, StringBuilder sb, String prefix) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            sb.append(prefix);
            appendNodeText(item, sb);
        }
    }

    private void appendOrderedListItems(JsonNode items, StringBuilder sb) {
        if (items == null || !items.isArray()) {
            return;
        }
        int index = 1;
        for (JsonNode item : items) {
            sb.append(index++).append(". ");
            appendNodeText(item, sb);
        }
    }

    private void appendChildren(JsonNode children, StringBuilder sb) {
        if (children == null || !children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            appendNodeText(child, sb);
        }
    }

    public String normalizeSectionHeader(String line) {
        return line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
    }
}
