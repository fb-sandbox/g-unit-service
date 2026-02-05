package com.fullbay.idptemplatefun.integration;

import com.fullbay.idp.piton.BaseIntegrationTest;
import com.fullbay.idp.piton.payload.PayloadBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example custom Lambda test showing how to test specific business logic.
 * Tests the getSha functionality of the Lambda function.
 *
 * This demonstrates the pattern for teams to create their own custom tests
 * with specific payloads and validation logic.
 *
 * Uses PitonTag enum for test categorization:
 * - prod: Critical tests that run in all environments including production
 * - preProd: Comprehensive tests that run in dev/qa/staging only
 *
 * NOTE: Tags likely should be at the METHOD level, not class level, to avoid inheritance issues.
 *
 * <h3>Accessing AWS Resources</h3>
 * The {stackId}-integ-test role is automatically assumed during setup.
 * Use the {@code integTestCreds} field for accessing stack-specific AWS resources:
 */
public class CustomLambdaTest extends BaseIntegrationTest {

    // ============================================================================
    // AWS Client Configuration Example
    // ============================================================================
    // The integTestCreds field is automatically available with {stackId}-integ-test role permissions
    //
    // private static final S3Client s3 = S3Client.builder()
    //     .region(Region.US_WEST_2)
    //     .credentialsProvider(integTestCreds)
    //     .build();
    //
    // private static final DynamoDbClient dynamoDb = DynamoDbClient.builder()
    //     .region(Region.US_WEST_2)
    //     .credentialsProvider(integTestCreds)
    //     .build();
    // ============================================================================

    @Test
    @Tag("prod")
    @Tag("preProd")
    @Execution(ExecutionMode.CONCURRENT)
    public void testGetShaCommand() {
        System.out.println("Testing custom getSha command...");

        if (lambdaClient == null) {
            System.err.println("WARNING: lambdaClient is not set, skipping test");
            return;
        }

        if (stackId == null) {
            System.err.println("WARNING: stackId is not set, skipping test");
            return;
        }

        // Create invoke request with "getSha" payload using PayloadBuilder
        // This is cleaner than manually constructing JSON strings
        InvokeRequest request = PayloadBuilder.simpleString("getSha")
            .toInvokeRequest(getLambdaFunctionName());

        // Invoke Lambda
        InvokeResponse response = lambdaClient.invoke(request);

        // Validate response
        assertNotNull(response, "Response should not be null");
        assertEquals(200, response.statusCode(), "Status code should be 200");
        assertNull(response.functionError(), "Function should not have errors");

        String responsePayload = response.payload().asUtf8String();
        System.out.println("Lambda response: " + responsePayload);

        // Validate response contains SHA pattern
        assertTrue(responsePayload.contains("Current SHA: "),
            "Response should contain 'Current SHA: '");
        assertTrue(responsePayload.matches(".*[a-f0-9]{7}.*"),
            "Response should contain a 7-character SHA");

        System.out.println("✅ Custom getSha test passed - demonstrates custom payload testing");
    }

    /**
     * Example: Testing with a JSON object payload.
     * Demonstrates how to send structured data to your Lambda.
     */
    @Test
    @Tag("preProd")
    @Execution(ExecutionMode.CONCURRENT)
    public void testJsonObjectPayload() {
        System.out.println("Testing Lambda with JSON object payload...");

        if (lambdaClient == null || stackId == null) {
            System.err.println("WARNING: lambdaClient or stackId not set, skipping test");
            return;
        }

        // Example: Send a JSON object payload
        var data = java.util.Map.of(
            "command", "process",
            "value", "test data"
        );

        InvokeRequest request = PayloadBuilder.json(data)
            .toInvokeRequest(getLambdaFunctionName());

        InvokeResponse response = lambdaClient.invoke(request);

        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertNull(response.functionError());

        System.out.println("✅ JSON object payload test passed");
    }

    /**
     * Example: Testing with API Gateway event payload.
     * This shows how to simulate an API Gateway request to your Lambda.
     * Note: Your Lambda needs to be configured to handle API Gateway events.
     */
    @Test
    @Tag("manual-only")  // Tagged manual-only as this requires specific Lambda configuration
    public void testApiGatewayPayloadExample() {
        System.out.println("Example: Testing with API Gateway payload...");

        if (lambdaClient == null || stackId == null) {
            System.err.println("WARNING: lambdaClient or stackId not set, skipping test");
            return;
        }

        // Create an API Gateway GET request
        InvokeRequest request = PayloadBuilder.apiGateway()
            .path("/api/resource")
            .httpMethod("GET")
            .header("Authorization", "Bearer test-token")
            .queryParam("filter", "active")
            .toInvokeRequest(getLambdaFunctionName());

        // Uncomment to test (requires Lambda configured for API Gateway)
        // InvokeResponse response = lambdaClient.invoke(request);
        // assertNotNull(response);

        System.out.println("✅ API Gateway payload example created successfully");
    }

    /**
     * Example: Testing with SNS event payload.
     * This shows how to simulate an SNS notification to your Lambda.
     * Note: Your Lambda needs to be configured to handle SNS events.
     */
    @Test
    @Tag("manual-only")  // Tagged manual-only as this requires specific Lambda configuration
    public void testSnsPayloadExample() {
        System.out.println("Example: Testing with SNS payload...");

        if (lambdaClient == null || stackId == null) {
            System.err.println("WARNING: lambdaClient or stackId not set, skipping test");
            return;
        }

        // Create an SNS notification
        InvokeRequest request = PayloadBuilder.sns()
            .message("Test notification message")
            .subject("Test Subject")
            .messageAttribute("eventType", "test.event")
            .toInvokeRequest(getLambdaFunctionName());

        // Uncomment to test (requires Lambda configured for SNS)
        // InvokeResponse response = lambdaClient.invoke(request);
        // assertNotNull(response);

        System.out.println("✅ SNS payload example created successfully");
    }

    /**
     * Example: Testing with SQS event payload.
     * This shows how to simulate an SQS message to your Lambda.
     * Note: Your Lambda needs to be configured to handle SQS events.
     */
    @Test
    @Tag("manual-only")  // Tagged manual-only as this requires specific Lambda configuration
    public void testSqsPayloadExample() {
        System.out.println("Example: Testing with SQS payload...");

        if (lambdaClient == null || stackId == null) {
            System.err.println("WARNING: lambdaClient or stackId not set, skipping test");
            return;
        }

        // Create an SQS message
        InvokeRequest request = PayloadBuilder.sqs()
            .body("Process this message")
            .messageAttribute("priority", "high")
            .toInvokeRequest(getLambdaFunctionName());

        // Uncomment to test (requires Lambda configured for SQS)
        // InvokeResponse response = lambdaClient.invoke(request);
        // assertNotNull(response);

        System.out.println("✅ SQS payload example created successfully");
    }

    /**
     * Example test that demonstrates tag filtering.
     * This test is tagged "manual-only" and should NEVER run in automated pipeline deployments.
     * If this test executes in the pipeline, it will FAIL, indicating tag filtering is broken.
     */
    @Test
    @Tag("manual-only")
    public void testManualOnlyExample() {
        System.err.println("❌ ERROR: This test should NEVER run in the automated pipeline!");
        System.err.println("❌ This test is tagged 'manual-only' which is NOT in ENVIRONMENT_TAG_MAPPING.");
        System.err.println("❌ If you see this message, tag filtering is BROKEN!");

        throw new AssertionError("This test is tagged 'manual-only' and should not run in the pipeline. " +
                "If this test executed, the tag filtering configuration is not working correctly.");
    }
}
