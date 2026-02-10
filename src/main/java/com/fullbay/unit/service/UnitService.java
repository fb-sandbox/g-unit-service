package com.fullbay.unit.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullbay.unit.exception.DuplicateVinException;
import com.fullbay.unit.exception.UnitNotFoundException;
import com.fullbay.unit.integration.nhtsa.NHTSAClient;
import com.fullbay.unit.integration.nhtsa.NHTSAMapper;
import com.fullbay.unit.integration.nhtsa.NHTSAVinDecodeResponse;
import com.fullbay.unit.integration.parts.PartsApiResponse;
import com.fullbay.unit.integration.parts.PartsMake;
import com.fullbay.unit.integration.parts.PartsModel;
import com.fullbay.unit.integration.parts.PartsServiceClient;
import com.fullbay.unit.integration.parts.PartsVehicle;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.model.entity.Vehicle;
import com.fullbay.unit.repository.UnitRepository;
import com.fullbay.unit.repository.VehicleRepository;
import com.fullbay.unit.util.IdGenerator;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Service for Unit business logic. */
@ApplicationScoped
@Slf4j
public class UnitService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};
    private static final Set<String> UNIT_FIELD_NAMES =
            Set.of("unitId", "customerId", "vin", "attributes", "createdAt", "updatedAt");

    private final UnitRepository unitRepository;
    private final VehicleRepository vehicleRepository;
    private final NHTSAClient nhtsaClient;
    private final PartsServiceClient partsServiceClient;
    private final ObjectMapper objectMapper;

    public UnitService(
            UnitRepository unitRepository,
            VehicleRepository vehicleRepository,
            @RestClient NHTSAClient nhtsaClient,
            @RestClient PartsServiceClient partsServiceClient,
            ObjectMapper objectMapper) {
        this.unitRepository = unitRepository;
        this.vehicleRepository = vehicleRepository;
        this.nhtsaClient = nhtsaClient;
        this.partsServiceClient = partsServiceClient;
        this.objectMapper = objectMapper;
    }

    /** SnapStart warmup: initialize service on startup. */
    @PostConstruct
    public void warmup() {
        log.info("UnitService initialized");
    }

    /**
     * Create a new Unit from VIN by calling NHTSA API. Saves vehicle data as a separate VIN# item
     * and the unit association as a slim UNT# item.
     *
     * @param vin The VIN to decode
     * @param customerId The customer ID
     * @return The created unit enriched with vehicle data
     * @throws DuplicateVinException if VIN already exists for this customer
     */
    public Unit createUnitFromVin(String vin, String customerId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-createUnitFromVin")) {
            segment.putAnnotation("customerId", customerId);
            segment.putAnnotation("vin", vin);

            // Check for duplicate VIN within customer
            final List<Unit> existing = unitRepository.findByCustomerIdAndVin(customerId, vin);
            if (!existing.isEmpty()) {
                log.warn("Duplicate VIN detected for customer {}: {}", customerId, vin);
                throw new DuplicateVinException(vin);
            }

            // Generate ID
            final String unitId = IdGenerator.generateUnitId();
            segment.putAnnotation("unitId", unitId);
            log.debug("Generated unit ID: {}", unitId);

            // Call NHTSA API to decode VIN
            log.debug("Calling NHTSA API for VIN: {}", vin);
            final NHTSAVinDecodeResponse nhtsaResponse = nhtsaClient.decodeVin(vin, "json");

            // Map NHTSA response to Vehicle entity
            Vehicle vehicle = NHTSAMapper.toVehicle(nhtsaResponse, vin);
            if (vehicle == null) {
                log.error("Failed to map NHTSA response to vehicle for VIN: {}", vin);
                throw new IllegalStateException("NHTSA response mapping failed for VIN: " + vin);
            }

            // Enrich with VCDB IDs from parts-service
            vehicle = enrichVehicleWithVcdbIds(vehicle);

            // Save vehicle data as VIN# item
            vehicleRepository.save(vehicle);

            // Build slim unit association and save as UNT# item
            final java.time.Instant now = java.time.Instant.now();
            final Unit unit =
                    Unit.builder()
                            .unitId(unitId)
                            .customerId(customerId)
                            .vin(vin)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
            unitRepository.save(unit);
            log.info("Created unit from VIN: {}", unitId);

            // Return enriched unit with vehicle data for the API response
            return enrichWithVehicle(unit, vehicle);
        }
    }

    /**
     * Get a Unit by ID, enriched with vehicle data.
     *
     * @param unitId The unit ID
     * @return The unit enriched with vehicle data
     * @throws UnitNotFoundException if unit not found
     */
    public Unit getUnitById(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitById")) {
            segment.putAnnotation("unitId", unitId);

            final Unit unit =
                    unitRepository
                            .findById(unitId)
                            .orElseThrow(
                                    () -> {
                                        log.warn("Unit not found: {}", unitId);
                                        return new UnitNotFoundException(unitId);
                                    });

            final Optional<Vehicle> vehicle = vehicleRepository.findByVin(unit.vin());
            log.debug("Retrieved unit: {}", unitId);
            return enrichWithVehicle(unit, vehicle.orElse(null));
        }
    }

    /**
     * Get Units by Customer ID and VIN, enriched with vehicle data.
     *
     * @param customerId The customer ID
     * @param vin The VIN
     * @return List of matching units
     */
    public List<Unit> getUnitByCustomerIdAndVin(String customerId, String vin) {
        try (Subsegment segment =
                AWSXRay.beginSubsegment("unit-service-getUnitByCustomerIdAndVin")) {
            segment.putAnnotation("customerId", customerId);
            segment.putAnnotation("vin", vin);

            final List<Unit> units = unitRepository.findByCustomerIdAndVin(customerId, vin);
            log.debug("Found {} units for customer: {} vin: {}", units.size(), customerId, vin);
            return enrichWithVehicles(units);
        }
    }

    /**
     * Get Units by VIN (across all customers), enriched with vehicle data.
     *
     * @param vin The VIN to search for
     * @return List of matching units
     */
    public List<Unit> getUnitsByVin(String vin) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitsByVin")) {
            segment.putAnnotation("vin", vin);

            final List<Unit> units = unitRepository.findByVin(vin);
            log.debug("Found {} units for vin: {}", units.size(), vin);
            return enrichWithVehicles(units);
        }
    }

    /**
     * Get Units by Customer ID, enriched with vehicle data.
     *
     * @param customerId The customer ID
     * @return List of unit DTOs
     */
    public List<Unit> getUnitsByCustomerId(String customerId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitsByCustomerId")) {
            segment.putAnnotation("customerId", customerId);

            final List<Unit> units = unitRepository.findByCustomerId(customerId);
            log.debug("Retrieved {} units for customer: {}", units.size(), customerId);
            return enrichWithVehicles(units);
        }
    }

    /**
     * Update a Unit's association fields (customerId, vin, attributes). Vehicle data is read-only.
     *
     * @param unitId The unit ID
     * @param request The update request
     * @return The updated unit enriched with vehicle data
     * @throws UnitNotFoundException if unit not found
     */
    public Unit updateUnit(String unitId, UpdateUnitRequest request) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-updateUnit")) {
            segment.putAnnotation("unitId", unitId);

            final Unit entity =
                    unitRepository
                            .findById(unitId)
                            .orElseThrow(
                                    () -> {
                                        log.warn("Unit not found for update: {}", unitId);
                                        return new UnitNotFoundException(unitId);
                                    });

            // If VIN is being updated, check for duplicates within customer
            if (request.getVin() != null && !request.getVin().equals(entity.vin())) {
                final String targetCustomerId =
                        request.getCustomerId() != null
                                ? request.getCustomerId()
                                : entity.customerId();
                final List<Unit> duplicates =
                        unitRepository.findByCustomerIdAndVin(targetCustomerId, request.getVin());
                if (!duplicates.isEmpty()) {
                    log.warn(
                            "Duplicate VIN detected during update for customer {}: {}",
                            targetCustomerId,
                            request.getVin());
                    throw new DuplicateVinException(request.getVin());
                }
            }

            // Update only association fields
            Unit updated = entity;
            if (request.getCustomerId() != null) {
                updated = updated.withCustomerId(request.getCustomerId());
            }
            if (request.getVin() != null) {
                updated = updated.withVin(request.getVin());
            }
            if (request.getAttributes() != null) {
                updated = updated.withAttributes(request.getAttributes());
            }

            // Update timestamp
            updated = updated.withUpdatedAt(java.time.Instant.now());

            unitRepository.update(updated);
            log.info("Updated unit: {}", unitId);

            // Return enriched with vehicle data from the (possibly new) VIN
            final Optional<Vehicle> vehicle = vehicleRepository.findByVin(updated.vin());
            return enrichWithVehicle(updated, vehicle.orElse(null));
        }
    }

    /**
     * Delete a Unit association. Vehicle data (VIN# item) is left for other units sharing the VIN.
     *
     * @param unitId The unit ID
     * @throws UnitNotFoundException if unit not found
     */
    public void deleteUnit(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-deleteUnit")) {
            segment.putAnnotation("unitId", unitId);

            // Verify existence before deletion
            unitRepository
                    .findById(unitId)
                    .orElseThrow(
                            () -> {
                                log.warn("Unit not found for deletion: {}", unitId);
                                return new UnitNotFoundException(unitId);
                            });

            unitRepository.delete(unitId);
            log.info("Deleted unit: {}", unitId);
        }
    }

    /**
     * Call parts-service to resolve VCDB baseVehicleId from year/make/model. Non-fatal: if the call
     * fails or no match is found, the vehicle is returned unchanged.
     */
    private Vehicle enrichVehicleWithVcdbIds(Vehicle vehicle) {
        if (vehicle.year() == null || vehicle.make() == null || vehicle.model() == null) {
            log.debug("Skipping VCDB lookup - missing year/make/model");
            return vehicle;
        }

        try (Subsegment segment = AWSXRay.beginSubsegment("parts-service-vcdb-lookup")) {
            segment.putAnnotation("year", vehicle.year());
            segment.putAnnotation("make", vehicle.make());
            segment.putAnnotation("model", vehicle.model());

            // Step 1: Resolve make name → makeId
            final PartsApiResponse<java.util.List<PartsMake>> makesResponse =
                    partsServiceClient.findMakesByName(vehicle.make());
            if (makesResponse == null
                    || makesResponse.getData() == null
                    || makesResponse.getData().isEmpty()) {
                log.info("No VCDB make match for: {}", vehicle.make());
                return vehicle;
            }
            final String makeId = makesResponse.getData().get(0).getMakeId();

            // Step 2: Resolve model name → modelId
            final PartsApiResponse<java.util.List<PartsModel>> modelsResponse =
                    partsServiceClient.findModelsByName(makeId, vehicle.model());
            if (modelsResponse == null
                    || modelsResponse.getData() == null
                    || modelsResponse.getData().isEmpty()) {
                log.info("No VCDB model match for: {} (makeId={})", vehicle.model(), makeId);
                return vehicle;
            }
            final String modelId = modelsResponse.getData().get(0).getModelId();

            // Step 3: Resolve year/makeId/modelId → baseVehicleId
            final PartsApiResponse<java.util.List<PartsVehicle>> vehiclesResponse =
                    partsServiceClient.findVehicles(
                            String.valueOf(vehicle.year()), makeId, modelId);
            if (vehiclesResponse == null
                    || vehiclesResponse.getData() == null
                    || vehiclesResponse.getData().isEmpty()) {
                log.info(
                        "No VCDB vehicle match for year={}, makeId={}, modelId={}",
                        vehicle.year(),
                        makeId,
                        modelId);
                return vehicle;
            }

            final PartsVehicle match = vehiclesResponse.getData().get(0);
            log.info(
                    "VCDB match: baseVehicleId={}, makeId={}, modelId={}",
                    match.getBaseVehicleId(),
                    makeId,
                    modelId);

            Vehicle enriched = vehicle;
            if (match.getBaseVehicleId() != null) {
                enriched = enriched.withBaseVehicleId(Integer.parseInt(match.getBaseVehicleId()));
                segment.putAnnotation("baseVehicleId", match.getBaseVehicleId());
            }
            if (makeId != null) {
                enriched = enriched.withMakeId(Integer.parseInt(makeId));
            }
            if (modelId != null) {
                enriched = enriched.withModelId(Integer.parseInt(modelId));
            }
            return enriched;
        } catch (Exception e) {
            log.warn(
                    "Parts-service VCDB lookup failed, continuing without VCDB IDs: {}",
                    e.getMessage());
            return vehicle;
        }
    }

    /**
     * Merge vehicle data into a Unit using ObjectMapper. Unit's own fields (unitId, customerId,
     * vin, attributes, timestamps) take precedence over vehicle fields.
     */
    private Unit enrichWithVehicle(Unit unit, Vehicle vehicle) {
        if (vehicle == null) {
            return unit;
        }
        final Map<String, Object> unitMap = objectMapper.convertValue(unit, MAP_TYPE_REF);
        final Map<String, Object> vehicleMap = objectMapper.convertValue(vehicle, MAP_TYPE_REF);
        // Remove unit-specific keys from vehicle map so unit's own fields take precedence
        vehicleMap.keySet().removeAll(UNIT_FIELD_NAMES);
        unitMap.putAll(vehicleMap);
        return objectMapper.convertValue(unitMap, Unit.class);
    }

    /** Enrich a list of units with vehicle data. Deduplicates VINs for efficient batch lookup. */
    private List<Unit> enrichWithVehicles(List<Unit> units) {
        if (units.isEmpty()) {
            return units;
        }
        final Set<String> vins =
                units.stream().map(Unit::vin).filter(v -> v != null).collect(Collectors.toSet());
        final Map<String, Vehicle> vehicleMap = vehicleRepository.findByVins(vins);
        return units.stream().map(u -> enrichWithVehicle(u, vehicleMap.get(u.vin()))).toList();
    }
}
