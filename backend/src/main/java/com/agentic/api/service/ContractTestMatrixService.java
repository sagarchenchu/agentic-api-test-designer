package com.agentic.api.service;

import com.agentic.api.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ContractTestMatrixService {

    public TestMatrixResponse generateFromContract(ApiContractDto contract) {
        List<TestCaseDto> cases = new ArrayList<>();
        int id = 1;

        String successStatus = contract.getResponses().stream()
                .map(ApiResponseDto::getStatusCode)
                .filter(code -> code.startsWith("2"))
                .findFirst()
                .orElse("201");

        cases.add(new TestCaseDto(
                formatId(id++),
                "Valid request for " + safe(contract.getSummary(), contract.getOperationId()),
                "Positive",
                "Valid body",
                successStatus,
                "Request accepted",
                "High",
                "Ready"
        ));

        ApiRequestBodyDto requestBody = contract.getRequestBody();
        if (requestBody != null) {
            for (String fieldName : requestBody.getRequiredFields()) {
                cases.add(new TestCaseDto(
                        formatId(id++),
                        "Missing required field: " + fieldName,
                        "Negative",
                        fieldName + " missing",
                        "400",
                        "Validation error for " + fieldName,
                        "High",
                        "Ready"
                ));
            }

            for (ApiFieldDto field : requestBody.getFields()) {
                if (isPrimitive(field.getType())) {
                    cases.add(new TestCaseDto(
                            formatId(id++),
                            "Invalid type for field: " + field.getName(),
                            "Negative",
                            field.getName() + " = invalid_" + field.getType(),
                            "400",
                            "Type validation error",
                            "Medium",
                            "Ready"
                    ));
                }

                if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
                    cases.add(new TestCaseDto(
                            formatId(id++),
                            "Invalid enum for field: " + field.getName(),
                            "Negative",
                            field.getName() + " = INVALID",
                            "400",
                            "Enum validation error",
                            "Medium",
                            "Ready"
                    ));
                }

                if (field.getMinimum() != null) {
                    cases.add(new TestCaseDto(
                            formatId(id++),
                            "Below minimum for field: " + field.getName(),
                            "Boundary",
                            field.getName() + " = " + (field.getMinimum() - 0.01),
                            "400",
                            "Minimum validation error",
                            "Medium",
                            "Ready"
                    ));
                }
            }
        }

        boolean authRequired = contract.getRequiredHeaders().stream()
                .anyMatch(h -> "Authorization".equalsIgnoreCase(h.getName()) && h.isRequired());
        if (authRequired) {
            cases.add(new TestCaseDto(
                    formatId(id++),
                    "Missing authorization header",
                    "Security",
                    "No auth token",
                    "401",
                    "Unauthorized",
                    "High",
                    "Ready"
            ));
        }

        ApiResponseDto successResponse = contract.getResponses().stream()
                .filter(r -> r.getStatusCode().startsWith("2"))
                .findFirst()
                .orElse(null);
        if (successResponse != null && successResponse.getFields() != null && !successResponse.getFields().isEmpty()) {
            cases.add(new TestCaseDto(
                    formatId(id++),
                    "Response schema validation",
                    "Schema",
                    "Validate response fields: " + String.join(", ", successResponse.getFields()),
                    successResponse.getStatusCode(),
                    "Response matches schema",
                    "High",
                    "Ready"
            ));
        }

        return new TestMatrixResponse(cases, new ArrayList<>());
    }

    private String formatId(int id) {
        return String.format(Locale.ROOT, "TC_%03d", id);
    }

    private boolean isPrimitive(String type) {
        if (type == null) {
            return false;
        }
        return Set.of("string", "number", "integer", "boolean").contains(type);
    }

    private String safe(String summary, String operationId) {
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        if (operationId != null && !operationId.isBlank()) {
            return operationId;
        }
        return "operation";
    }
}
