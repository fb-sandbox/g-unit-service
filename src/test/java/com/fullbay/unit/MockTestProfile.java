package com.fullbay.unit;

import com.fullbay.util.JacksonConverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.Map;

/** Test profile that provides mock DynamoDB beans for testing. */
public class MockTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Collections.singletonMap("dynamodb.table.name", "g-unit-service-test");
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }

    /** Alternative mock configuration for DynamoDB. */
    @Alternative
    @ApplicationScoped
    public static class MockDynamoDbConfig {

        @Produces
        @ApplicationScoped
        @Alternative
        public DynamoDbClient dynamoDbClient() {
            // Return a mock client that doesn't connect to AWS
            return DynamoDbClient.builder().region(Region.US_WEST_2).build();
        }

        @Produces
        @ApplicationScoped
        @Alternative
        public JacksonConverter jacksonConverter() {
            return new JacksonConverter();
        }
    }
}
