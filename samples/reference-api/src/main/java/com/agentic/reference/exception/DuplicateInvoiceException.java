package com.agentic.reference.exception;

public class DuplicateInvoiceException extends RuntimeException {

    public DuplicateInvoiceException(String invoiceId) {
        super("Duplicate invoiceId: " + invoiceId);
    }
}
