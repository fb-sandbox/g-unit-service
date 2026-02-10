package com.fullbay.unit.util;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;

import java.util.List;

/** Shared Athena query helpers for starting, polling, and retrieving query results. */
@Slf4j
public final class AthenaQueryHelper {

    private static final long POLL_INTERVAL_MS = 500;
    private static final int MAX_POLL_ATTEMPTS = 60;

    private AthenaQueryHelper() {
        // Utility class
    }

    /**
     * Start an Athena query execution.
     *
     * @param athenaClient The Athena client
     * @param sql The SQL query string
     * @param workgroup The Athena workgroup
     * @param database The Glue catalog database
     * @param outputLocation The S3 output location for results
     * @return The query execution ID
     */
    public static String startQuery(
            AthenaClient athenaClient,
            String sql,
            String workgroup,
            String database,
            String outputLocation) {
        final StartQueryExecutionRequest request =
                StartQueryExecutionRequest.builder()
                        .queryString(sql)
                        .workGroup(workgroup)
                        .queryExecutionContext(
                                QueryExecutionContext.builder().database(database).build())
                        .resultConfiguration(
                                ResultConfiguration.builder()
                                        .outputLocation(outputLocation)
                                        .build())
                        .build();

        final StartQueryExecutionResponse response = athenaClient.startQueryExecution(request);
        return response.queryExecutionId();
    }

    /**
     * Poll Athena until the query completes or fails.
     *
     * @param athenaClient The Athena client
     * @param queryExecutionId The query execution ID to poll
     */
    public static void waitForQueryCompletion(AthenaClient athenaClient, String queryExecutionId) {
        final GetQueryExecutionRequest request =
                GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build();

        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            final GetQueryExecutionResponse response = athenaClient.getQueryExecution(request);
            final QueryExecutionState state = response.queryExecution().status().state();

            if (state == QueryExecutionState.SUCCEEDED) {
                return;
            }
            if (state == QueryExecutionState.FAILED || state == QueryExecutionState.CANCELLED) {
                final String reason = response.queryExecution().status().stateChangeReason();
                throw new RuntimeException("Athena query " + state + ": " + reason);
            }

            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Athena query", e);
            }
        }

        throw new RuntimeException(
                "Athena query timed out after " + MAX_POLL_ATTEMPTS + " poll attempts");
    }

    /**
     * Retrieve the result rows from a completed Athena query, skipping the header row.
     *
     * @param athenaClient The Athena client
     * @param queryExecutionId The query execution ID
     * @return List of data rows (header row excluded)
     */
    public static List<Row> getResultRows(AthenaClient athenaClient, String queryExecutionId) {
        final GetQueryResultsRequest request =
                GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build();

        final GetQueryResultsResponse response = athenaClient.getQueryResults(request);
        final List<Row> rows = response.resultSet().rows();

        // First row is always the header — skip it
        if (rows.size() < 2) {
            return List.of();
        }
        return rows.subList(1, rows.size());
    }

    /**
     * Escape single quotes in SQL string values.
     *
     * @param input The raw string
     * @return The escaped string safe for SQL interpolation
     */
    public static String escapeSql(String input) {
        return input.replace("'", "''");
    }
}
