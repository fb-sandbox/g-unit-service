package com.fullbay.idptemplatefun.integration;

import com.fullbay.idp.piton.PitonTestRunner;
import com.fullbay.idp.piton.TestRunConfig;
import com.fullbay.idp.piton.tests.StandardLambdaInvocationTests;

import java.util.List;

/**
 * Main entry point for Piton integration tests.
 * This class is invoked by the Piton Integration Test Lambda after deployment.
 *
 * The builderFromEnvironment() automatically applies tag filtering based on the ENVIRONMENT system property:
 * - dev, qa, staging: Runs preProd tests (comprehensive test suite)
 * - prod: Runs prod tests only (critical smoke tests)
 */
public class PitonIntegrationTests {

    public static void main(String[] args) {
        TestRunConfig config = TestRunConfig.builderFromEnvironment()
            .testClasses(List.of(
                StandardLambdaInvocationTests.class,  // Standard test - validates Lambda is responsive
                CustomLambdaTest.class                // Custom tests (includes manual-only example)
            ))
            .build();

        int exitCode = PitonTestRunner.runTests(config);
        System.exit(exitCode);
    }
}