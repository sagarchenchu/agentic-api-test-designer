package com.agentic.reference.exception;

public class SimulatedServerException extends RuntimeException {

    public SimulatedServerException() {
        super("Simulated internal server error");
    }
}
