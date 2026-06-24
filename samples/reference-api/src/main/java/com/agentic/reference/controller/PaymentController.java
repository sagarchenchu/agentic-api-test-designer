package com.agentic.reference.controller;

import com.agentic.reference.exception.InvalidRequestException;
import com.agentic.reference.model.CreatePaymentRequest;
import com.agentic.reference.model.PaymentResponse;
import com.agentic.reference.model.UpdatePaymentStatusRequest;
import com.agentic.reference.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Reference payments API for agentic-api-test-designer validation")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create payment", operationId = "createPayment")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestHeader(value = "X-Client-Id", required = false) String clientId,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            throw new InvalidRequestException("Content-Type must be application/json");
        }

        PaymentResponse response = paymentService.createPayment(request, authorization, correlationId, clientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get payment", operationId = "getPayment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not Found")
    })
    public PaymentResponse getPayment(@PathVariable String paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @PutMapping(value = "/{paymentId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update payment status", operationId = "updatePaymentStatus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "404", description = "Not Found")
    })
    public PaymentResponse updatePaymentStatus(
            @PathVariable String paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return paymentService.updateStatus(paymentId, request);
    }
}
