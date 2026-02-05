package com.fullbay.unit.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

/** Global configuration for the quarkus application. Automatically runs on application startup. */
@ApplicationScoped
@Slf4j
public class StartupConfig {
    /**
     * Imports the FullBay CA cert into the java truststore.
     *
     * @throws Exception
     */
    @PostConstruct
    public void initialize() {
        log.info("Unit Service startup configuration initialized");
        // Certificate import and other FullBay-specific setup can be added here
        // Currently using AWS Lambda and DynamoDB without additional certificate requirements
    }
}
