package com.fullbay.unit.config;

import com.fullbay.util.JacksonConverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Configuration for DynamoDB client and converters. */
@ApplicationScoped
@Slf4j
public class DynamoDbConfig {

    /**
     * Produces DynamoDbClient bean.
     *
     * @return DynamoDbClient configured for AWS region us-west-2
     */
    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        log.info("Initializing DynamoDbClient for region: US_WEST_2");
        return DynamoDbClient.builder().region(Region.US_WEST_2).build();
    }

    /**
     * Produces JacksonConverter bean for JSON serialization/deserialization.
     *
     * @return JacksonConverter configured with helper object mapper
     */
    @Produces
    @ApplicationScoped
    public JacksonConverter jacksonConverter() {
        log.debug("Initializing JacksonConverter");
        return new JacksonConverter();
    }
}
