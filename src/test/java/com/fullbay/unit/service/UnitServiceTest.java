package com.fullbay.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fullbay.unit.exception.DuplicateVinException;
import com.fullbay.unit.exception.UnitNotFoundException;
import com.fullbay.unit.integration.nhtsa.NHTSAClient;
import com.fullbay.unit.model.dto.CreateUnitRequest;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.repository.UnitRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    private UnitService service;

    @Mock UnitRepository repository;
    @Mock NHTSAClient nhtsaClient;

    private Unit testEntity;
    private CreateUnitRequest createRequest;

    @BeforeEach
    void setUp() {
        service = new UnitService(repository, nhtsaClient);

        Instant now = Instant.now();
        testEntity =
                Unit.builder()
                        .unitId("unt-abc1234")
                        .customerId("cst-xyz789")
                        .vin("1HGCM82633A004352")
                        .year(2020)
                        .make("Honda")
                        .model("Accord")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        createRequest =
                CreateUnitRequest.builder()
                        .customerId("cst-xyz789")
                        .vin("1HGCM82633A004352")
                        .year(2020)
                        .make("Honda")
                        .model("Accord")
                        .build();
    }

    @Test
    void shouldCreateUnit() {
        when(repository.findByVin(anyString())).thenReturn(List.of());
        doNothing().when(repository).save(any());

        Unit result = service.createUnit(createRequest);

        assertNotNull(result);
        assertEquals("cst-xyz789", result.customerId());
        assertEquals("1HGCM82633A004352", result.vin());
        verify(repository).findByVin("1HGCM82633A004352");
        verify(repository).save(any());
    }

    @Test
    void shouldThrowDuplicateVinException() {
        when(repository.findByVin("1HGCM82633A004352")).thenReturn(List.of(testEntity));

        assertThrows(DuplicateVinException.class, () -> service.createUnit(createRequest));
        verify(repository).findByVin("1HGCM82633A004352");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldGetUnitById() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));

        Unit result = service.getUnitById("unt-abc1234");

        assertNotNull(result);
        assertEquals("unt-abc1234", result.unitId());
        verify(repository).findById("unt-abc1234");
    }

    @Test
    void shouldThrowUnitNotFoundById() {
        when(repository.findById("unt-invalid")).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class, () -> service.getUnitById("unt-invalid"));
        verify(repository).findById("unt-invalid");
    }

    @Test
    void shouldGetUnitByVin() {
        when(repository.findByVin("1HGCM82633A004352")).thenReturn(List.of(testEntity));

        Unit result = service.getUnitByVin("1HGCM82633A004352");

        assertNotNull(result);
        assertEquals("1HGCM82633A004352", result.vin());
        verify(repository).findByVin("1HGCM82633A004352");
    }

    @Test
    void shouldReturnNullForMissingVin() {
        when(repository.findByVin("INVALID")).thenReturn(List.of());

        Unit result = service.getUnitByVin("INVALID");

        assertNull(result);
        verify(repository).findByVin("INVALID");
    }

    @Test
    void shouldGetUnitsByCustomerId() {
        when(repository.findByCustomerId("cst-xyz789")).thenReturn(List.of(testEntity));

        List<Unit> results = service.getUnitsByCustomerId("cst-xyz789");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("unt-abc1234", results.get(0).unitId());
        verify(repository).findByCustomerId("cst-xyz789");
    }

    @Test
    void shouldUpdateUnit() {
        when(repository.findById("unt-abc1234")).thenReturn(Optional.of(testEntity));

        UpdateUnitRequest updateRequest = UpdateUnitRequest.builder().year(2021).build();

        Unit result = service.updateUnit("unt-abc1234", updateRequest);

        assertNotNull(result);
        assertEquals(2021, result.year());
        verify(repository).findById("unt-abc1234");
        verify(repository).update(any());
    }

    @Test
    void shouldThrowUnitNotFoundOnUpdate() {
        when(repository.findById("unt-invalid")).thenReturn(Optional.empty());

        UpdateUnitRequest updateRequest = UpdateUnitRequest.builder().year(2021).build();

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
