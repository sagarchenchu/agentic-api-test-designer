package com.agentic.reference.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class OpenApiStaticController {

    @GetMapping(value = "/openapi/reference-api.yaml", produces = "application/yaml")
    public ResponseEntity<String> referenceOpenApiYaml() throws IOException {
        String body = StreamUtils.copyToString(
                new ClassPathResource("openapi/reference-api.yaml").getInputStream(),
                StandardCharsets.UTF_8
        );
        return ResponseEntity.ok().contentType(MediaType.valueOf("application/yaml")).body(body);
    }
}
