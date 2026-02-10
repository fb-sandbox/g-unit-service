package com.fullbay.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fullbay.unit.exception.UnitNotFoundException;
import com.fullbay.unit.integration.nhtsa.NHTSAClient;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.model.entity.Vehicle;
import com.fullbay.unit.repository.UnitRepository;
import com.fullbay.unit.repository.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    private UnitService service;

    @Mock UnitRepository repository;
    @Mock VehicleRepository vehicleRepository;
    @Mock NHTSAClient nhtsaClient;

    private ObjectMapper objectMapper;
    private Unit testEntity;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        service = new UnitService(repository, vehicleRepository, nhtsaClient, objectMapper);

        final Instant now = Instant.now();
        testEntity =
                Unit.builder()
                        .unitId("unt-abc1234")
                        .customerId("cst-xyz789")
                        .vin("1HGCM82633A004352")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        testVehicle =
                Vehicle.builder()
                        .vin("1HGCM82633A004352")
                        .year(2020)
                        .make("Honda")
                        .model("Accord")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
    }

    @Test
    void shouldGetUnitByIdEnrichedWithVehicle() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));
        when(vehicleRepository.findByVin("1HGCM82633A004352")).thenReturn(Optional.of(testVehicle));

        final Unit result = service.getUnitById("unt-abc1234");

        assertNotNull(result);
        assertEquals("unt-abc1234", result.unitId());
        assertEquals("cst-xyz789", result.customerId());
        assertEquals("1HGCM82633A004352", result.vin());
        assertEquals(2020, result.year());
        assertEquals("Honda", result.make());
        assertEquals("Accord", result.model());
        verify(repository).findById("unt-abc1234");
        verify(vehicleRepository).findByVin("1HGCM82633A004352");
    }

    @Test
    void shouldGetUnitByIdWithMissingVehicle() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));
        when(vehicleRepository.findByVin("1HGCM82633A004352")).thenReturn(Optional.empty());

        final Unit result = service.getUnitById("unt-abc1234");

        assertNotNull(result);
        assertEquals("unt-abc1234", result.unitId());
        assertNull(result.year());
        assertNull(result.make());
        verify(repository).findById("unt-abc1234");
        verify(vehicleRepository).findByVin("1HGCM82633A004352");
    }

    @Test
    void shouldThrowUnitNotFoundById() {
        when(repository.findById("unt-invalid")).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class, () -> service.getUnitById("unt-invalid"));
        verify(repository).findById("unt-invalid");
    }

    @Test
    void shouldGetUnitByCustomerIdAndVinEnriched() {
        when(repository.findByCustomerIdAndVin("cst-xyz789", "1HGCM82633A004352"))
                .thenReturn(List.of(testEntity));
        when(vehicleRepository.findByVins(Set.of("1HGCM82633A004352")))
                .thenReturn(Map.of("1HGCM82633A004352", testVehicle));

        final List<Unit> results =
                service.getUnitByCustomerIdAndVin("cst-xyz789", "1HGCM82633A004352");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1HGCM82633A004352", results.get(0).vin());
        assertEquals(2020, results.get(0).year());
        verify(repository).findByCustomerIdAndVin("cst-xyz789", "1HGCM82633A004352");
        verify(vehicleRepository).findByVins(Set.of("1HGCM82633A004352"));
    }

    @Test
    void shouldReturnEmptyForMissingCustomerIdAndVin() {
        when(repository.findByCustomerIdAndVin("cst-xyz789", "INVALID")).thenReturn(List.of());

        final List<Unit> results = service.getUnitByCustomerIdAndVin("cst-xyz789", "INVALID");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(repository).findByCustomerIdAndVin("cst-xyz789", "INVALID");
    }

    @Test
    void shouldGetUnitsByCustomerIdEnriched() {
        when(repository.findByCustomerId("cst-xyz789")).thenReturn(List.of(testEntity));
        when(vehicleRepository.findByVins(Set.of("1HGCM82633A004352")))
                .thenReturn(Map.of("1HGCM82633A004352", testVehicle));

        final List<Unit> results = service.getUnitsByCustomerId("cst-xyz789");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("unt-abc1234", results.get(0).unitId());
        assertEquals("Honda", results.get(0).make());
        verify(repository).findByCustomerId("cst-xyz789");
    }

    @Test
    void shouldGetUnitsByVinEnriched() {
        when(repository.findByVin("1HGCM82633A004352")).thenReturn(List.of(testEntity));
        when(vehicleRepository.findByVins(Set.of("1HGCM82633A004352")))
                .thenReturn(Map.of("1HGCM82633A004352", testVehicle));

        final List<Unit> results = service.getUnitsByVin("1HGCM82633A004352");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1HGCM82633A004352", results.get(0).vin());
        assertEquals(2020, results.get(0).year());
        verify(repository).findByVin("1HGCM82633A004352");
    }

    @Test
    void shouldReturnEmptyForMissingVin() {
        when(repository.findByVin("INVALID")).thenReturn(List.of());

        final List<Unit> results = service.getUnitsByVin("INVALID");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(repository).findByVin("INVALID");
    }

    @Test
    void shouldUpdateUnitAssociationFields() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));
        when(vehicleRepository.findByVin("1HGCM82633A004352")).thenReturn(Optional.of(testVehicle));

        final UpdateUnitRequest updateRequest =
                UpdateUnitRequest.builder().attributes(Map.of("color", "red")).build();

        final Unit result = service.updateUnit("unt-abc1234", updateRequest);

        assertNotNull(result);
        assertEquals("unt-abc1234", result.unitId());
        assertEquals(Map.of("color", "red"), result.attributes());
        assertEquals(2020, result.year());
        verify(repository).findById("unt-abc1234");
        verify(repository).update(any());
    }

    @Test
    void shouldThrowUnitNotFoundOnUpdate() {
        when(repository.findById("unt-invalid")).thenReturn(Optional.empty());

        final UpdateUnitRequest updateRequest =
                UpdateUnitRequest.builder().attributes(Map.of("color", "red")).build();

        assertThrows(
                UnitNotFoundException.class,
                () -> service.updateUnit("unt-invalid", updateRequest));
        verify(repository).findById("unt-invalid");
        verify(repository, never()).update(any());
    }

    @Test
    void shouldDeleteUnit() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));

        service.deleteUnit("unt-abc1234");

        verify(repository).findById("unt-abc1234");
        verify(repository).delete("unt-abc1234");
    }

    @Test
    void shouldThrowUnitNotFoundOnDelete() {
        when(repository.findById("unt-invalid")).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class, () -> service.deleteUnit("unt-invalid"));
        verify(repository).findById("unt-invalid");
        verify(repository, never()).delete(any());
    }
}
