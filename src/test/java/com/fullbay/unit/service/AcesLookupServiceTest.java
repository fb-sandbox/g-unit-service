package com.fullbay.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fullbay.unit.model.dto.PartDto;

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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AcesLookupServiceTest {

    @Mock AthenaClient athenaClient;

    private AcesLookupService service;

    @BeforeEach
    void setUp() {
        service =
                new AcesLookupService(
                        athenaClient,
                        "g-acespies",
                        "g_acespies",
                        "s3://g-acespies-345594586248-us-west-2/athena-results/");
    }

    @Test
    void shouldReturnPartsOnSuccessfulLookup() {
        mockAthenaQueryFlow(
                row(
                        "part_number",
                        "brand_name",
                        "category_name",
                        "part_terminology_name",
                        "position",
                        "qty",
                        "note"),
                row(
                        "BP-123",
                        "Bosch",
                        "Brakes",
                        "Disc Brake Pad Set",
                        "Front",
                        "2",
                        "OE replacement"),
                row("BP-456", "Wagner", "Brakes", "Brake Rotor", "Rear", "2", null));

        final List<PartDto> parts = service.findPartsForVehicle(156741, null, null);

        assertEquals(2, parts.size());
        assertEquals("BP-123", parts.get(0).getPartNumber());
        assertEquals("Bosch", parts.get(0).getBrandName());
        assertEquals("Brakes", parts.get(0).getCategory());
        assertEquals("Disc Brake Pad Set", parts.get(0).getPartType());
        assertEquals("Front", parts.get(0).getPosition());
        assertEquals(2, parts.get(0).getQuantity());
        assertEquals("OE replacement", parts.get(0).getNote());
        assertEquals("BP-456", parts.get(1).getPartNumber());
        assertEquals("Wagner", parts.get(1).getBrandName());
        assertEquals("Brake Rotor", parts.get(1).getPartType());
        assertNull(parts.get(1).getNote());
        verify(athenaClient).startQueryExecution(any(StartQueryExecutionRequest.class));
    }

    @Test
    void shouldReturnCategoriesOnSuccessfulLookup() {
        mockAthenaQueryFlow(
                row("category"), row("Air Filter"), row("Brake Pad"), row("Oil Filter"));

        final List<String> categories = service.findCategoriesForVehicle(156741, null);

        assertEquals(3, categories.size());
        assertEquals("Air Filter", categories.get(0));
        assertEquals("Brake Pad", categories.get(1));
        assertEquals("Oil Filter", categories.get(2));
    }

    @Test
    void shouldReturnEmptyPartsWhenNoResults() {
        mockAthenaQueryFlow(
                row(
                        "part_number",
                        "brand_name",
                        "category_name",
                        "part_terminology_name",
                        "position",
                        "qty",
                        "note"));

        final List<PartDto> parts = service.findPartsForVehicle(156741, null, null);

        assertTrue(parts.isEmpty());
    }

    @Test
    void shouldReturnEmptyCategoriesWhenNoResults() {
        mockAthenaQueryFlow(row("category"));

        final List<String> categories = service.findCategoriesForVehicle(156741, null);

        assertTrue(categories.isEmpty());
    }

    @Test
    void shouldReturnEmptyPartsWhenBaseVehicleIdIsNull() {
        final List<PartDto> parts = service.findPartsForVehicle(null, null, null);

        assertTrue(parts.isEmpty());
        verifyNoInteractions(athenaClient);
    }

    @Test
    void shouldReturnEmptyCategoriesWhenBaseVehicleIdIsNull() {
        final List<String> categories = service.findCategoriesForVehicle(null, null);

        assertTrue(categories.isEmpty());
        verifyNoInteractions(athenaClient);
    }

    @Test
    void shouldReturnEmptyPartsOnAthenaError() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("Athena unavailable"));

        final List<PartDto> parts = service.findPartsForVehicle(156741, null, null);

        assertTrue(parts.isEmpty());
    }

    @Test
    void shouldReturnEmptyCategoriesOnAthenaError() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("Athena unavailable"));

        final List<String> categories = service.findCategoriesForVehicle(156741, null);

        assertTrue(categories.isEmpty());
    }

    @Test
    void shouldApplyCategoryFilter() {
        mockAthenaQueryFlow(
                row(
                        "part_number",
                        "brand_name",
                        "category_name",
                        "part_terminology_name",
                        "position",
                        "qty",
                        "note"),
                row("BP-123", "Bosch", "Brakes", "Disc Brake Pad Set", "Front", "2", null));

        final List<PartDto> parts = service.findPartsForVehicle(156741, null, "Brakes");

        assertEquals(1, parts.size());
        assertEquals("Brakes", parts.get(0).getCategory());
        assertEquals("Disc Brake Pad Set", parts.get(0).getPartType());
        verify(athenaClient).startQueryExecution(any(StartQueryExecutionRequest.class));
    }

    @Test
    void shouldReturnEmptyPartsWhenQueryFails() {
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

        final List<PartDto> parts = service.findPartsForVehicle(156741, null, null);

        assertTrue(parts.isEmpty());
    }

    @Test
    void shouldReturnAllCategories() {
        mockAthenaQueryFlow(
                row("category"), row("Air Filter"), row("Brake Pad"), row("Oil Filter"));

        final List<String> categories = service.findAllCategories();

        assertEquals(3, categories.size());
        assertEquals("Air Filter", categories.get(0));
        assertEquals("Brake Pad", categories.get(1));
        assertEquals("Oil Filter", categories.get(2));
    }

    @Test
    void shouldReturnEmptyAllCategoriesOnAthenaError() {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenThrow(new RuntimeException("Athena unavailable"));

        final List<String> categories = service.findAllCategories();

        assertTrue(categories.isEmpty());
    }

    private void mockAthenaQueryFlow(Row... rows) {
        when(athenaClient.startQueryExecution(any(StartQueryExecutionRequest.class)))
                .thenReturn(
                        StartQueryExecutionResponse.builder().queryExecutionId("qid-123").build());

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

        when(athenaClient.getQueryResults(any(GetQueryResultsRequest.class)))
                .thenReturn(
                        GetQueryResultsResponse.builder()
                                .resultSet(ResultSet.builder().rows(rows).build())
                                .build());
    }

    private static Row row(String... values) {
        final Datum[] data = new Datum[values.length];
        for (int i = 0; i < values.length; i++) {
            data[i] = Datum.builder().varCharValue(values[i]).build();
        }
        return Row.builder().data(data).build();
    }
}
