package com.agentic.api.security;

import com.agentic.api.config.SecurityProperties;
import com.agentic.api.model.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Agentic-Token";

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public ApiTokenAuthFilter(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path.equals("/api/agent/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!securityProperties.isConfigured()) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Security is enabled but AGENTIC_API_TOKEN is not configured", "SECURITY_MISCONFIGURED");
            return;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.equals(securityProperties.getApiToken())) {
            writeError(response, HttpStatus.UNAUTHORIZED,
                    "Missing or invalid API token", "UNAUTHORIZED");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String message,
            String code
    ) throws IOException {
        ApiErrorResponse body = ApiErrorResponse.of("Unauthorized", message, code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
