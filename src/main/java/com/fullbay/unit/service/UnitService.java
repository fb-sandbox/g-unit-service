package com.fullbay.unit.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.exception.DuplicateVinException;
import com.fullbay.unit.exception.UnitNotFoundException;
import com.fullbay.unit.integration.nhtsa.NHTSAClient;
import com.fullbay.unit.integration.nhtsa.NHTSAMapper;
import com.fullbay.unit.integration.nhtsa.NHTSAVinDecodeResponse;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.repository.UnitRepository;
import com.fullbay.unit.util.IdGenerator;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/** Service for Unit business logic. */
@ApplicationScoped
@Slf4j
public class UnitService {

    private final UnitRepository unitRepository;
    private final NHTSAClient nhtsaClient;

    public UnitService(UnitRepository unitRepository, @RestClient NHTSAClient nhtsaClient) {
        this.unitRepository = unitRepository;
        this.nhtsaClient = nhtsaClient;
    }

    /** SnapStart warmup: initialize service on startup. */
    @PostConstruct
    public void warmup() {
        log.info("UnitService initialized");
    }

    /**
     * Create a new Unit from VIN by calling NHTSA API.
     *
     * @param vin The VIN to decode
     * @param customerId The customer ID
     * @return The created unit DTO
     * @throws DuplicateVinException if VIN already exists
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

            // Map NHTSA response to entity
            final Unit entity = NHTSAMapper.toEntity(nhtsaResponse, vin, customerId, unitId);
            if (entity == null) {
                log.error("Failed to map NHTSA response to entity for VIN: {}", vin);
                throw new IllegalStateException("NHTSA response mapping failed for VIN: " + vin);
            }

            unitRepository.save(entity);
            log.info("Created unit from VIN: {}", unitId);

            return entity;
        }
    }

    /**
     * Get a Unit by ID.
     *
     * @param unitId The unit ID
     * @return The unit DTO
     * @throws UnitNotFoundException if unit not found
     */
    public Unit getUnitById(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitById")) {
            segment.putAnnotation("unitId", unitId);

            final Unit entity =
                    unitRepository
                            .findById(unitId)
                            .orElseThrow(
                                    () -> {
                                        log.warn("Unit not found: {}", unitId);
                                        return new UnitNotFoundException(unitId);
                                    });

            log.debug("Retrieved unit: {}", unitId);
            return entity;
        }
    }

    /**
     * Get Units by Customer ID and VIN.
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

            final List<Unit> entities = unitRepository.findByCustomerIdAndVin(customerId, vin);
            log.debug("Found {} units for customer: {} vin: {}", entities.size(), customerId, vin);
            return entities;
        }
    }

    /**
     * Get Units by VIN (across all customers).
     *
     * @param vin The VIN to search for
     * @return List of matching units
     */
    public List<Unit> getUnitsByVin(String vin) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitsByVin")) {
            segment.putAnnotation("vin", vin);

            final List<Unit> entities = unitRepository.findByVin(vin);
            log.debug("Found {} units for vin: {}", entities.size(), vin);
            return entities;
        }
    }

    /**
     * Get Units by Customer ID.
     *
     * @param customerId The customer ID
     * @return List of unit DTOs
     */
    public List<Unit> getUnitsByCustomerId(String customerId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitsByCustomerId")) {
            segment.putAnnotation("customerId", customerId);

            final List<Unit> entities = unitRepository.findByCustomerId(customerId);
            log.debug("Retrieved {} units for customer: {}", entities.size(), customerId);
            return entities;
        }
    }

    /**
     * Update a Unit.
     *
     * @param unitId The unit ID
     * @param request The update request
     * @return The updated unit DTO
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
                final List<Unit> existing =
                        unitRepository.findByCustomerIdAndVin(targetCustomerId, request.getVin());
                if (!existing.isEmpty()) {
                    log.warn(
                            "Duplicate VIN detected during update for customer {}: {}",
                            targetCustomerId,
                            request.getVin());
                    throw new DuplicateVinException(request.getVin());
                }
            }

            // Use @With to create immutable copy with updates
            Unit updated = entity;
            if (request.getCustomerId() != null) {
                updated = updated.withCustomerId(request.getCustomerId());
            }
            if (request.getVin() != null) {
                updated = updated.withVin(request.getVin());
            }
            if (request.getYear() != null) {
                updated = updated.withYear(request.getYear());
            }
            if (request.getMake() != null) {
                updated = updated.withMake(request.getMake());
            }
            if (request.getManufacturer() != null) {
                updated = updated.withManufacturer(request.getManufacturer());
            }
            if (request.getModel() != null) {
                updated = updated.withModel(request.getModel());
            }
            if (request.getTrim() != null) {
                updated = updated.withTrim(request.getTrim());
            }
            if (request.getSubmodel() != null) {
                updated = updated.withSubmodel(request.getSubmodel());
            }
            if (request.getUnitType() != null) {
                updated = updated.withUnitType(request.getUnitType());
            }
            if (request.getVehicleType() != null) {
                updated = updated.withVehicleType(request.getVehicleType());
            }
            if (request.getBodyClass() != null) {
                updated = updated.withBodyClass(request.getBodyClass());
            }
            if (request.getBodyType() != null) {
                updated = updated.withBodyType(request.getBodyType());
            }
            if (request.getFuelType() != null) {
                updated = updated.withFuelType(request.getFuelType());
            }
            if (request.getEngineType() != null) {
                updated = updated.withEngineType(request.getEngineType());
            }
            if (request.getEngineManufacturer() != null) {
                updated = updated.withEngineManufacturer(request.getEngineManufacturer());
            }
            if (request.getEngineModel() != null) {
                updated = updated.withEngineModel(request.getEngineModel());
            }
            if (request.getEngineCylinders() != null) {
                updated = updated.withEngineCylinders(request.getEngineCylinders());
            }
            if (request.getEngineHP() != null) {
                updated = updated.withEngineHP(request.getEngineHP());
            }
            if (request.getEngineHPMax() != null) {
                updated = updated.withEngineHPMax(request.getEngineHPMax());
            }
            if (request.getDisplacementLiters() != null) {
                updated = updated.withDisplacementLiters(request.getDisplacementLiters());
            }
            if (request.getEngineConfiguration() != null) {
                updated = updated.withEngineConfiguration(request.getEngineConfiguration());
            }
            if (request.getOtherEngineInfo() != null) {
                updated = updated.withOtherEngineInfo(request.getOtherEngineInfo());
            }
            if (request.getTransmissionType() != null) {
                updated = updated.withTransmissionType(request.getTransmissionType());
            }
            if (request.getDriveType() != null) {
                updated = updated.withDriveType(request.getDriveType());
            }
            if (request.getBrakeSystemType() != null) {
                updated = updated.withBrakeSystemType(request.getBrakeSystemType());
            }
            if (request.getDoors() != null) {
                updated = updated.withDoors(request.getDoors());
            }
            if (request.getSeats() != null) {
                updated = updated.withSeats(request.getSeats());
            }
            if (request.getCurbWeightLB() != null) {
                updated = updated.withCurbWeightLB(request.getCurbWeightLB());
            }
            if (request.getGvwr() != null) {
                updated = updated.withGvwr(request.getGvwr());
            }
            if (request.getBedLengthIN() != null) {
                updated = updated.withBedLengthIN(request.getBedLengthIN());
            }
            if (request.getWheelbaseIN() != null) {
                updated = updated.withWheelbaseIN(request.getWheelbaseIN());
            }
            if (request.getPlantCity() != null) {
                updated = updated.withPlantCity(request.getPlantCity());
            }
            if (request.getPlantState() != null) {
                updated = updated.withPlantState(request.getPlantState());
            }
            if (request.getPlantCountry() != null) {
                updated = updated.withPlantCountry(request.getPlantCountry());
            }
            if (request.getSeatBelts() != null) {
                updated = updated.withSeatBelts(request.getSeatBelts());
            }
            if (request.getAirBagsFront() != null) {
                updated = updated.withAirBagsFront(request.getAirBagsFront());
            }
            if (request.getAttributes() != null) {
                updated = updated.withAttributes(request.getAttributes());
            }

            // Update timestamp
            updated = updated.withUpdatedAt(java.time.Instant.now());

            unitRepository.update(updated);
            log.info("Updated unit: {}", unitId);

            return updated;
        }
    }

    /**
     * Delete a Unit.
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
}
