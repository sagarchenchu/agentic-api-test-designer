package com.agentic.reference.model;

import jakarta.validation.constraints.NotBlank;

public class PaymentMetadata {

    @NotBlank
    private String invoiceId;

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }
}
