package com.fullbay.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VcdbLookupServiceTest {

    @Mock AthenaClient athenaClient;

    private VcdbLookupService service;

    @BeforeEach
    void setUp() {
        service =
                new VcdbLookupService(
                        athenaClient,
                        "g-acespies",
                        "g_acespies",
                        "s3://g-acespies-345594586248-us-west-2/athena-results/");
    }

    @Test
    void shouldReturnBaseVehicleIdOnSuccessfulLookup() {
        mockAthenaQueryFlow("156741");

        final Optional<Integer> result = service.lookupBaseVehicleId(2020, "Ford", "F-150");

        assertTrue(result.isPresent());
        assertEquals(156741, result.get());
        verify(athenaClient).startQueryExecution(any(StartQueryExecutionRequest.class));
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        // Start query
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(
                        StartQueryExecutionResponse.builder().queryExecutionId("qid-123").build());

        // Poll - succeeded
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(
                        GetQueryExecutionResponse.builder()
                                .queryExecution(
                                        QueryExecution.builder()
                                                .status(
                                                        QueryExecutionStatus.builder()
                                                                .state(
                                                                        QueryExecutionState
                                                                                .SUCCEEDED)
                                                                .build())
                                                .build())
                                .build());

        // Results - header only, no data rows
        final Row headerRow =
                Row.builder().data(Datum.builder().varCharValue("base_vehicle_id").build()).build();
        when(athenaClient.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(
                        GetQueryResultsResponse.builder()
                                .resultSet(ResultSet.builder().rows(headerRow).build())
                                .build());

        final Optional<Integer> result = service.lookupBaseVehicleId(2020, "Unknown", "Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenYearIsNull() {
        final Optional<Integer> result = service.lookupBaseVehicleId(null, "Ford", "F-150");

        assertTrue(result.isEmpty());
        verifyNoInteractions(athenaClient);
    }

    @Test
    void shouldReturnEmptyWhenMakeIsNull() {
        final Optional<Integer> result = service.lookupBaseVehicleId(2020, null, "F-150");

        assertTrue(result.isEmpty());
        verifyNoInteractions(athenaClient);
    }

    @Test
    void shouldReturnEmptyWhenModelIsNull() {
        final Optional<Integer> result = service.lookupBaseVehicleId(2020, "Ford", null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(athenaClient);
    }

    @Test
    void shouldReturnEmptyOnAthenaError() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("Athena unavailable"));

        final Optional<Integer> result = service.lookupBaseVehicleId(2020, "Ford", "F-150");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenQueryFails() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(
                        StartQueryExecutionResponse.builder().queryExecutionId("qid-fail").build());

        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(
                        GetQueryExecutionResponse.builder()
                                .queryExecution(
                                        QueryExecution.builder()
                                                .status(
                                                        QueryExecutionStatus.builder()
                                                                .state(QueryExecutionState.FAILED)
                                                                .stateChangeReason(
                                                                        "Table not found")
                                                                .build())
                                                .build())
                                .build());

        final Optional<Integer> result = service.lookupBaseVehicleId(2020, "Ford", "F-150");

        assertTrue(result.isEmpty());
    }

    private void mockAthenaQueryFlow(String baseVehicleIdValue) {
        // Start query
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(
                        StartQueryExecutionResponse.builder().queryExecutionId("qid-123").build());

        // Poll - succeeded
        when(athenaClient.getQueryExecution(any(GetQueryExecutionRequest.class)))
                .thenReturn(
                        GetQueryExecutionResponse.builder()
                                .queryExecution(
                                        QueryExecution.builder()
                                                .status(
                                                        QueryExecutionStatus.builder()
                                                                .state(
                                                                        QueryExecutionState
                                                                                .SUCCEEDED)
                                                                .build())
                                                .build())
                                .build());

        // Results
        final Row headerRow =
                Row.builder().data(Datum.builder().varCharValue("base_vehicle_id").build()).build();
        final Row dataRow =
                Row.builder()
                        .data(Datum.builder().varCharValue(baseVehicleIdValue).build())
                        .build();
        when(athenaClient.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(
                        GetQueryResultsResponse.builder()
                                .resultSet(ResultSet.builder().rows(headerRow, dataRow).build())
                                .build());
    }
}
