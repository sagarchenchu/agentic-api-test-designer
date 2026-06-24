package com.agentic.api.service;

import com.agentic.api.exception.ContractNotFoundException;
import com.agentic.api.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class OpenApiParserService {

    private static final String JSON_CONTENT = "application/json";
    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "GET", "POST", "PUT", "PATCH", "DELETE"
    );

    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlObjectMapper;
    private final RestClient restClient;

    public OpenApiParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        this.restClient = RestClient.create();
    }

    public ApiContractDto extractContract(AgentRequest request) {
        JsonNode spec = loadSpec(request);
        String method = request.getHttpMethod().toUpperCase(Locale.ROOT);
        String userPath = normalizePath(request.getEndpointPath());

        if (!SUPPORTED_METHODS.contains(method)) {
            throw new ContractNotFoundException("Unsupported HTTP method: " + method);
        }

        JsonNode paths = spec.path("paths");
        if (!paths.isObject() || paths.isEmpty()) {
            throw new ContractNotFoundException("No paths found in OpenAPI specification");
        }

        Map.Entry<String, JsonNode> matched = findMatchingPath(paths, userPath, method);
        if (matched == null) {
            throw new ContractNotFoundException(
                    "No operation found for " + method + " " + userPath
            );
        }

        String swaggerPath = matched.getKey();
        JsonNode operation = matched.getValue();

        ApiContractDto contract = new ApiContractDto();
        contract.setEndpointPath(swaggerPath);
        contract.setHttpMethod(method);
        contract.setOperationId(textOrNull(operation, "operationId"));
        contract.setSummary(textOrNull(operation, "summary"));
        contract.setDescription(textOrNull(operation, "description"));
        contract.setTags(extractTags(operation));
        extractParameters(operation, spec, contract);
        contract.setRequestBody(extractRequestBody(operation, spec));
        contract.setResponses(extractResponses(operation, spec));

        if (contract.getRequestBody() == null && hasRequestBodyMethod(method)) {
            contract.getWarnings().add("No application/json request body defined for this operation");
        }

        return contract;
    }

    public Optional<ApiContractDto> tryExtractContract(AgentRequest request) {
        try {
            return Optional.of(extractContract(request));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private JsonNode loadSpec(AgentRequest request) {
        try {
            if (request.getSwaggerJson() != null && !request.getSwaggerJson().isBlank()) {
                return parseSpecBody(request.getSwaggerJson());
            }
            if (request.getSwaggerUrl() != null && !request.getSwaggerUrl().isBlank()) {
                String body = restClient.get()
                        .uri(request.getSwaggerUrl())
                        .retrieve()
                        .body(String.class);
                if (body == null || body.isBlank()) {
                    throw new IllegalArgumentException("Swagger URL returned empty response");
                }
                return parseSpecBody(body);
            }
            throw new IllegalArgumentException("Either swaggerJson or swaggerUrl is required");
        } catch (ContractNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to load OpenAPI specification: " + ex.getMessage(), ex);
        }
    }

    private JsonNode parseSpecBody(String body) throws JsonProcessingException {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException jsonError) {
            if (looksLikeYaml(body)) {
                return yamlObjectMapper.readTree(body);
            }
            throw jsonError;
        }
    }

    private boolean looksLikeYaml(String body) {
        String trimmed = body.stripLeading();
        return trimmed.startsWith("openapi:")
                || trimmed.startsWith("swagger:")
                || (trimmed.contains(":\n") && !trimmed.startsWith("{"));
    }

    private Map.Entry<String, JsonNode> findMatchingPath(JsonNode paths, String userPath, String method) {
        String methodKey = method.toLowerCase(Locale.ROOT);

        Map.Entry<String, JsonNode> exact = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(paths.fields(), Spliterator.ORDERED), false)
                .filter(e -> pathsMatch(e.getKey(), userPath, false))
                .filter(e -> e.getValue().has(methodKey))
                .findFirst()
                .map(e -> Map.entry(e.getKey(), e.getValue().get(methodKey)))
                .orElse(null);

        if (exact != null) {
            return exact;
        }

        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(paths.fields(), Spliterator.ORDERED), false)
                .filter(e -> pathsMatch(e.getKey(), userPath, true))
                .filter(e -> e.getValue().has(methodKey))
                .findFirst()
                .map(e -> Map.entry(e.getKey(), e.getValue().get(methodKey)))
                .orElse(null);
    }

    boolean pathsMatch(String swaggerPath, String userPath, boolean allowTemplate) {
        String[] swaggerSegments = splitPath(swaggerPath);
        String[] userSegments = splitPath(userPath);

        if (swaggerSegments.length != userSegments.length) {
            return false;
        }

        for (int i = 0; i < swaggerSegments.length; i++) {
            String sw = swaggerSegments[i];
            String us = userSegments[i];
            if (sw.equals(us)) {
                continue;
            }
            if (allowTemplate && sw.startsWith("{") && sw.endsWith("}")) {
                continue;
            }
            return false;
        }
        return true;
    }

    private String[] splitPath(String path) {
        String normalized = normalizePath(path);
        if ("/".equals(normalized)) {
            return new String[]{""};
        }
        return normalized.split("/");
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private List<String> extractTags(JsonNode operation) {
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = operation.path("tags");
        if (tagsNode.isArray()) {
            tagsNode.forEach(tag -> tags.add(tag.asText()));
        }
        return tags;
    }

    private void extractParameters(JsonNode operation, JsonNode spec, ApiContractDto contract) {
        JsonNode parameters = operation.path("parameters");
        if (!parameters.isArray()) {
            return;
        }

        for (JsonNode param : parameters) {
            JsonNode resolved = resolveNode(param, spec);
            String location = textOrNull(resolved, "in");
            ApiParameterDto dto = new ApiParameterDto();
            dto.setName(textOrNull(resolved, "name"));
            dto.setIn(location);
            dto.setRequired(resolved.path("required").asBoolean(false));
            dto.setDescription(textOrNull(resolved, "description"));

            JsonNode schema = resolveNode(resolved.path("schema"), spec);
            dto.setType(textOrNull(schema, "type"));
            dto.setFormat(textOrNull(schema, "format"));
            dto.setExample(extractExample(schema));

            switch (location != null ? location : "") {
                case "header" -> contract.getRequiredHeaders().add(dto);
                case "path" -> contract.getPathParams().add(dto);
                case "query" -> contract.getQueryParams().add(dto);
                default -> {
                }
            }
        }
    }

    private ApiRequestBodyDto extractRequestBody(JsonNode operation, JsonNode spec) {
        JsonNode requestBody = operation.path("requestBody");
        if (requestBody.isMissingNode() || requestBody.isNull()) {
            return null;
        }

        JsonNode resolvedBody = resolveNode(requestBody, spec);
        JsonNode jsonContent = resolvedBody.path("content").path(JSON_CONTENT);
        if (jsonContent.isMissingNode()) {
            return null;
        }

        JsonNode schema = resolveNode(jsonContent.path("schema"), spec);
        ApiRequestBodyDto bodyDto = new ApiRequestBodyDto();
        bodyDto.setRequired(resolvedBody.path("required").asBoolean(false));
        bodyDto.setContentType(JSON_CONTENT);
        bodyDto.setFields(extractFields(schema, spec));
        bodyDto.setRequiredFields(bodyDto.getFields().stream()
                .filter(ApiFieldDto::isRequired)
                .map(ApiFieldDto::getName)
                .toList());
        return bodyDto;
    }

    private List<ApiResponseDto> extractResponses(JsonNode operation, JsonNode spec) {
        List<ApiResponseDto> responses = new ArrayList<>();
        JsonNode responsesNode = operation.path("responses");
        if (!responsesNode.isObject()) {
            return responses;
        }

        responsesNode.fields().forEachRemaining(entry -> {
            String statusCode = entry.getKey();
            JsonNode responseNode = resolveNode(entry.getValue(), spec);
            ApiResponseDto dto = new ApiResponseDto();
            dto.setStatusCode(statusCode);
            dto.setDescription(textOrNull(responseNode, "description"));

            JsonNode jsonContent = responseNode.path("content").path(JSON_CONTENT);
            if (!jsonContent.isMissingNode()) {
                dto.setContentType(JSON_CONTENT);
                JsonNode schema = resolveNode(jsonContent.path("schema"), spec);
                dto.setFields(extractFieldNames(schema, spec));
                dto.setRequiredFields(extractRequiredFieldNames(schema, spec));
            }
            responses.add(dto);
        });

        responses.sort(Comparator.comparing(ApiResponseDto::getStatusCode));
        return responses;
    }

    private List<ApiFieldDto> extractFields(JsonNode schema, JsonNode spec) {
        JsonNode resolved = resolveNode(schema, spec);
        if (!"object".equals(textOrNull(resolved, "type")) && !resolved.has("properties")) {
            return List.of();
        }

        Set<String> required = new HashSet<>();
        JsonNode requiredNode = resolved.path("required");
        if (requiredNode.isArray()) {
            requiredNode.forEach(node -> required.add(node.asText()));
        }

        JsonNode properties = resolved.path("properties");
        if (!properties.isObject()) {
            return List.of();
        }

        List<ApiFieldDto> fields = new ArrayList<>();
        properties.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode property = resolveNode(entry.getValue(), spec);
            ApiFieldDto field = new ApiFieldDto();
            field.setName(name);
            field.setRequired(required.contains(name));
            field.setType(textOrNull(property, "type"));
            field.setDescription(textOrNull(property, "description"));
            field.setExample(extractExample(property));
            field.setFormat(textOrNull(property, "format"));
            if (property.has("nullable")) {
                field.setNullable(property.path("nullable").asBoolean());
            }
            if (property.has("minimum")) {
                field.setMinimum(property.path("minimum").asDouble());
            }
            if (property.has("maximum")) {
                field.setMaximum(property.path("maximum").asDouble());
            }
            if (property.has("enum") && property.get("enum").isArray()) {
                List<String> enumValues = new ArrayList<>();
                property.get("enum").forEach(v -> enumValues.add(v.asText()));
                field.setEnumValues(enumValues);
            }
            fields.add(field);
        });
        return fields;
    }

    private List<String> extractFieldNames(JsonNode schema, JsonNode spec) {
        return extractFields(schema, spec).stream().map(ApiFieldDto::getName).toList();
    }

    private List<String> extractRequiredFieldNames(JsonNode schema, JsonNode spec) {
        return extractFields(schema, spec).stream()
                .filter(ApiFieldDto::isRequired)
                .map(ApiFieldDto::getName)
                .toList();
    }

    JsonNode resolveNode(JsonNode node, JsonNode spec) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return node;
        }
        if (node.has("$ref")) {
            String ref = node.get("$ref").asText();
            JsonNode resolved = resolveRef(ref, spec);
            if (resolved != null && !resolved.isMissingNode()) {
                return resolveNode(resolved, spec);
            }
        }
        if (node.has("allOf") && node.get("allOf").isArray()) {
            return mergeAllOf(node.get("allOf"), spec);
        }
        return node;
    }

    private JsonNode mergeAllOf(JsonNode allOf, JsonNode spec) {
        Map<String, JsonNode> mergedProperties = new LinkedHashMap<>();
        Set<String> required = new LinkedHashSet<>();
        String type = "object";

        for (JsonNode item : allOf) {
            JsonNode resolved = resolveNode(item, spec);
            if (resolved.has("type")) {
                type = resolved.get("type").asText();
            }
            JsonNode requiredNode = resolved.path("required");
            if (requiredNode.isArray()) {
                requiredNode.forEach(r -> required.add(r.asText()));
            }
            JsonNode properties = resolved.path("properties");
            if (properties.isObject()) {
                properties.fields().forEachRemaining(entry ->
                        mergedProperties.put(entry.getKey(), entry.getValue()));
            }
        }

        var merged = objectMapper.createObjectNode();
        merged.put("type", type);
        var requiredArray = merged.putArray("required");
        required.forEach(requiredArray::add);
        var propertiesNode = merged.putObject("properties");
        mergedProperties.forEach(propertiesNode::set);
        return merged;
    }

    private JsonNode resolveRef(String ref, JsonNode spec) {
        if (ref == null || !ref.startsWith("#/")) {
            return null;
        }
        String[] parts = ref.substring(2).split("/");
        JsonNode current = spec;
        for (String part : parts) {
            current = current.path(part);
        }
        return current;
    }

    private Object extractExample(JsonNode node) {
        if (node.has("example")) {
            return jsonToValue(node.get("example"));
        }
        if (node.has("default")) {
            return jsonToValue(node.get("default"));
        }
        JsonNode examples = node.path("examples");
        if (examples.isObject() && examples.size() > 0) {
            JsonNode first = examples.elements().next();
            if (first.has("value")) {
                return jsonToValue(first.get("value"));
            }
        }
        return null;
    }

    private Object jsonToValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        return node.toString();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private boolean hasRequestBodyMethod(String method) {
        return Set.of("POST", "PUT", "PATCH").contains(method);
    }
}
