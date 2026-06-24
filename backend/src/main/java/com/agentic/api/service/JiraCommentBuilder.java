package com.agentic.api.service;

import com.agentic.api.model.JiraLinkPrRequest;
import com.agentic.api.model.JiraPostSummaryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class JiraCommentBuilder {

    private final ObjectMapper objectMapper;

    public JiraCommentBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode buildSummaryComment(JiraPostSummaryRequest request) {
        ObjectNode doc = newDoc();
        ArrayNode content = doc.putArray("content");

        content.add(heading("Agentic API Test Designer Summary"));
        content.add(bulletParagraph("Generated Test Cases", String.valueOf(request.getTestCaseCount())));
        content.add(bulletParagraph("BDD Generated", request.isBddGenerated() ? "Yes" : "No"));
        content.add(bulletParagraph("Automation Files Written", String.valueOf(request.getFilesWritten())));

        if (request.getExecutionStatus() != null && !request.getExecutionStatus().isBlank()) {
            content.add(bulletParagraph("Execution Status", request.getExecutionStatus()));
            content.add(bulletParagraph("Passed", String.valueOf(request.getPassed())));
            content.add(bulletParagraph("Failed", String.valueOf(request.getFailed())));
        }

        if (request.getPrUrl() != null && !request.getPrUrl().isBlank()) {
            content.add(linkParagraph("Pull Request", request.getPrUrl()));
        }
        if (request.getSerenityReportPath() != null && !request.getSerenityReportPath().isBlank()) {
            content.add(bulletParagraph("Serenity Report", request.getSerenityReportPath()));
        }

        content.add(paragraph("Generated from Jira story + Swagger contract."));
        return wrapBody(doc);
    }

    public ObjectNode buildLinkPrComment(JiraLinkPrRequest request) {
        ObjectNode doc = newDoc();
        ArrayNode content = doc.putArray("content");

        content.add(heading("Pull Request Linked"));
        content.add(linkParagraph("Pull Request", request.getPrUrl()));
        content.add(paragraph("Linked by Agentic API Test Designer."));
        return wrapBody(doc);
    }

    private ObjectNode wrapBody(ObjectNode doc) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("body", doc);
        return body;
    }

    private ObjectNode newDoc() {
        ObjectNode doc = objectMapper.createObjectNode();
        doc.put("type", "doc");
        doc.put("version", 1);
        return doc;
    }

    private ObjectNode heading(String text) {
        ObjectNode heading = objectMapper.createObjectNode();
        heading.put("type", "heading");
        heading.putObject("attrs").put("level", 3);
        heading.putArray("content").add(textNode(text));
        return heading;
    }

    private ObjectNode paragraph(String text) {
        ObjectNode paragraph = objectMapper.createObjectNode();
        paragraph.put("type", "paragraph");
        paragraph.putArray("content").add(textNode(text));
        return paragraph;
    }

    private ObjectNode bulletParagraph(String label, String value) {
        ObjectNode paragraph = objectMapper.createObjectNode();
        paragraph.put("type", "paragraph");
        ArrayNode line = paragraph.putArray("content");
        line.add(strongText(label + ": "));
        line.add(textNode(value));
        return paragraph;
    }

    private ObjectNode linkParagraph(String label, String url) {
        ObjectNode paragraph = objectMapper.createObjectNode();
        paragraph.put("type", "paragraph");
        ArrayNode line = paragraph.putArray("content");
        line.add(strongText(label + ": "));
        line.add(linkNode(url));
        return paragraph;
    }

    private ObjectNode textNode(String text) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "text");
        node.put("text", text);
        return node;
    }

    private ObjectNode strongText(String text) {
        ObjectNode node = textNode(text);
        ArrayNode marks = node.putArray("marks");
        marks.addObject().put("type", "strong");
        return node;
    }

    private ObjectNode linkNode(String url) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "text");
        node.put("text", url);
        ArrayNode marks = node.putArray("marks");
        ObjectNode link = marks.addObject();
        link.put("type", "link");
        link.putObject("attrs").put("href", url);
        return node;
    }
}
