package com.agentic.reference.service;

import com.agentic.reference.config.ReferenceApiProperties;
import com.agentic.reference.exception.BusinessValidationException;
import com.agentic.reference.exception.DuplicateInvoiceException;
import com.agentic.reference.exception.ForbiddenException;
import com.agentic.reference.exception.InvalidRequestException;
import com.agentic.reference.exception.PaymentNotFoundException;
import com.agentic.reference.exception.SimulatedServerException;
import com.agentic.reference.exception.UnauthorizedException;
import com.agentic.reference.model.CreatePaymentRequest;
import com.agentic.reference.model.PaymentResponse;
import com.agentic.reference.model.UpdatePaymentStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PaymentService {

    private static final Set<String> ALLOWED_PAYMENT_METHOD_TYPES = Set.of("CARD", "ACH");

    private final ReferenceApiProperties properties;
    private final Map<String, PaymentResponse> paymentsById = new ConcurrentHashMap<>();
    private final Set<String> invoiceIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong paymentSequence = new AtomicLong(1000);

    public PaymentService(ReferenceApiProperties properties) {
        this.properties = properties;
    }

    public PaymentResponse createPayment(
            CreatePaymentRequest request,
            String authorization,
            String correlationId,
            String clientId
    ) {
        validateHeaders(authorization, correlationId, clientId);
        validateBusinessRules(request);

        String invoiceId = request.getMetadata().getInvoiceId();
        if (!invoiceIds.add(invoiceId)) {
            throw new DuplicateInvoiceException(invoiceId);
        }

        PaymentResponse payment = new PaymentResponse();
        payment.setPaymentId("PAY-" + paymentSequence.incrementAndGet());
        payment.setAccountId(request.getAccountId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
        payment.setStatus("CREATED");
        payment.setInvoiceId(invoiceId);
        payment.setCorrelationId(correlationId);
        payment.setClientId(clientId);
        payment.setCreatedAt(Instant.now());

        paymentsById.put(payment.getPaymentId(), payment);
        return payment;
    }

    public PaymentResponse getPayment(String paymentId) {
        PaymentResponse payment = paymentsById.get(paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    public PaymentResponse updateStatus(String paymentId, UpdatePaymentStatusRequest request) {
        PaymentResponse payment = getPayment(paymentId);
        payment.setStatus(request.getStatus().toUpperCase(Locale.ROOT));
        return payment;
    }

    public void reset() {
        paymentsById.clear();
        invoiceIds.clear();
        paymentSequence.set(1000);
    }

    private void validateHeaders(String authorization, String correlationId, String clientId) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!properties.getValidToken().equals(token)) {
            throw new UnauthorizedException("Invalid bearer token");
        }

        if (!StringUtils.hasText(correlationId)) {
            throw new InvalidRequestException("X-Correlation-Id header is required");
        }

        if (!StringUtils.hasText(clientId)) {
            throw new InvalidRequestException("X-Client-Id header is required");
        }

        if (properties.getTrigger500ClientId().equalsIgnoreCase(clientId)) {
            throw new SimulatedServerException();
        }

        if (properties.getForbiddenClientIds().stream()
                .anyMatch(forbidden -> forbidden.equalsIgnoreCase(clientId))) {
            throw new ForbiddenException("Client is not allowed to create payments");
        }
    }

    private void validateBusinessRules(CreatePaymentRequest request) {
        String currency = request.getCurrency().toUpperCase(Locale.ROOT);
        if (!properties.getSupportedCurrencies().contains(currency)) {
            throw new BusinessValidationException("Unsupported currency: " + request.getCurrency());
        }

        String paymentMethodType = request.getPaymentMethod().getType().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_METHOD_TYPES.contains(paymentMethodType)) {
            throw new BusinessValidationException("Unsupported payment method type: " + request.getPaymentMethod().getType());
        }
    }
}
