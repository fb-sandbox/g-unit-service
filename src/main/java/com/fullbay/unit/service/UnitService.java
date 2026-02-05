package com.fullbay.unit.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.exception.DuplicateVinException;
import com.fullbay.unit.exception.UnitNotFoundException;
import com.fullbay.unit.integration.nhtsa.NHTSAClient;
import com.fullbay.unit.integration.nhtsa.NHTSAMapper;
import com.fullbay.unit.integration.nhtsa.NHTSAVinDecodeResponse;
import com.fullbay.unit.model.dto.CreateUnitRequest;
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
     * Create a new Unit.
     *
     * @param request The create request
     * @return The created unit DTO
     * @throws DuplicateVinException if VIN already exists
     */
    public Unit createUnit(CreateUnitRequest request) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-createUnit")) {
            segment.putAnnotation("customerId", request.getCustomerId());
            segment.putAnnotation("vin", request.getVin());

            // Check for duplicate VIN
            final List<Unit> existing = unitRepository.findByVin(request.getVin());
            if (!existing.isEmpty()) {
                log.warn("Duplicate VIN detected: {}", request.getVin());
                throw new DuplicateVinException(request.getVin());
            }

            // Generate ID and create entity
            final String unitId = IdGenerator.generateUnitId();
            segment.putAnnotation("unitId", unitId);
            log.debug("Generated unit ID: {}", unitId);

            final java.time.Instant now = java.time.Instant.now();
            final Unit entity =
                    Unit.builder()
                            .unitId(unitId)
                            .customerId(request.getCustomerId())
                            .vin(request.getVin())
                            .year(request.getYear())
                            .make(request.getMake())
                            .makeId(request.getMakeId())
                            .manufacturer(request.getManufacturer())
                            .manufacturerId(request.getManufacturerId())
                            .model(request.getModel())
                            .modelId(request.getModelId())
                            .series(request.getSeries())
                            .trim(request.getTrim())
                            .trim2(request.getTrim2())
                            .submodel(request.getSubmodel())
                            .unitType(request.getUnitType())
                            .vehicleType(request.getVehicleType())
                            .bodyClass(request.getBodyClass())
                            .bodyType(request.getBodyType())
                            .bodyCabType(request.getBodyCabType())
                            .bedType(request.getBedType())
                            .busType(request.getBusType())
                            .busLength(request.getBusLength())
                            .busFloorConfigType(request.getBusFloorConfigType())
                            .motorcycleChassisType(request.getMotorcycleChassisType())
                            .motorcycleSuspensionType(request.getMotorcycleSuspensionType())
                            .trailerBodyType(request.getTrailerBodyType())
                            .trailerLength(request.getTrailerLength())
                            .trailerType(request.getTrailerType())
                            .customMotorcycleType(request.getCustomMotorcycleType())
                            .nonLandUse(request.getNonLandUse())
                            .otherBusInfo(request.getOtherBusInfo())
                            .otherMotorcycleInfo(request.getOtherMotorcycleInfo())
                            .otherTrailerInfo(request.getOtherTrailerInfo())
                            .fuelType(request.getFuelType())
                            .fuelTypeSecondary(request.getFuelTypeSecondary())
                            .engineType(request.getEngineType())
                            .engineManufacturer(request.getEngineManufacturer())
                            .engineModel(request.getEngineModel())
                            .engineCylinders(request.getEngineCylinders())
                            .engineHP(request.getEngineHP())
                            .engineHPMax(request.getEngineHPMax())
                            .engineKW(request.getEngineKW())
                            .displacementLiters(request.getDisplacementLiters())
                            .displacementCC(request.getDisplacementCC())
                            .displacementCI(request.getDisplacementCI())
                            .engineConfiguration(request.getEngineConfiguration())
                            .engineCycles(request.getEngineCycles())
                            .valveTrainDesign(request.getValveTrainDesign())
                            .fuelInjectionType(request.getFuelInjectionType())
                            .otherEngineInfo(request.getOtherEngineInfo())
                            .turbo(request.getTurbo())
                            .coolingType(request.getCoolingType())
                            .topSpeedMPH(request.getTopSpeedMPH())
                            .transmissionType(request.getTransmissionType())
                            .transmissionStyle(request.getTransmissionStyle())
                            .transmissionSpeeds(request.getTransmissionSpeeds())
                            .driveType(request.getDriveType())
                            .brakeSystemType(request.getBrakeSystemType())
                            .brakeSystemDesc(request.getBrakeSystemDesc())
                            .combinedBrakingSystem(request.getCombinedBrakingSystem())
                            .dynamicBrakeSupport(request.getDynamicBrakeSupport())
                            .axles(request.getAxles())
                            .axleConfiguration(request.getAxleConfiguration())
                            .doors(request.getDoors())
                            .windows(request.getWindows())
                            .seats(request.getSeats())
                            .seatRows(request.getSeatRows())
                            .curbWeightLB(request.getCurbWeightLB())
                            .gvwr(request.getGvwr())
                            .gvwrTo(request.getGvwrTo())
                            .gcwr(request.getGcwr())
                            .gcwrTo(request.getGcwrTo())
                            .bedLengthIN(request.getBedLengthIN())
                            .wheelbaseIN(request.getWheelbaseIN())
                            .wheelbaseShort(request.getWheelbaseShort())
                            .wheelbaseLong(request.getWheelbaseLong())
                            .wheelbaseType(request.getWheelbaseType())
                            .trackWidth(request.getTrackWidth())
                            .wheelSizeFront(request.getWheelSizeFront())
                            .wheelSizeRear(request.getWheelSizeRear())
                            .abs(request.getAbs())
                            .esc(request.getEsc())
                            .tractionControl(request.getTractionControl())
                            .forwardCollisionWarning(request.getForwardCollisionWarning())
                            .blindSpotMon(request.getBlindSpotMon())
                            .blindSpotIntervention(request.getBlindSpotIntervention())
                            .laneDepartureWarning(request.getLaneDepartureWarning())
                            .laneKeepSystem(request.getLaneKeepSystem())
                            .laneCenteringAssistance(request.getLaneCenteringAssistance())
                            .parkAssist(request.getParkAssist())
                            .rearCrossTrafficAlert(request.getRearCrossTrafficAlert())
                            .rearAutomaticEmergencyBraking(
                                    request.getRearAutomaticEmergencyBraking())
                            .rearVisibilitySystem(request.getRearVisibilitySystem())
                            .pedestrianAutomaticEmergencyBraking(
                                    request.getPedestrianAutomaticEmergencyBraking())
                            .seatBelts(request.getSeatBelts())
                            .seatBeltsAll(request.getSeatBeltsAll())
                            .pretensioner(request.getPretensioner())
                            .airBagsFront(request.getAirBagsFront())
                            .airBagsKnee(request.getAirBagsKnee())
                            .airBagsSide(request.getAirBagsSide())
                            .airBagsCurtain(request.getAirBagsCurtain())
                            .airBagsSeatCushion(request.getAirBagsSeatCushion())
                            .airbagLocFront(request.getAirbagLocFront())
                            .airbagLocKnee(request.getAirbagLocKnee())
                            .airbagLocSide(request.getAirbagLocSide())
                            .airbagLocCurtain(request.getAirbagLocCurtain())
                            .airbagLocSeatCushion(request.getAirbagLocSeatCushion())
                            .activeSafetyNote(request.getActiveSafetyNote())
                            .activeSafetySysNote(request.getActiveSafetySysNote())
                            .otherRestraintSystemInfo(request.getOtherRestraintSystemInfo())
                            .cib(request.getCib())
                            .edr(request.getEdr())
                            .batteryType(request.getBatteryType())
                            .batteryInfo(request.getBatteryInfo())
                            .evDriveUnit(request.getEvDriveUnit())
                            .electrificationLevel(request.getElectrificationLevel())
                            .batteryKWh(request.getBatteryKWh())
                            .batteryKWhTo(request.getBatteryKWhTo())
                            .batteryV(request.getBatteryV())
                            .batteryVTo(request.getBatteryVTo())
                            .batteryA(request.getBatteryA())
                            .batteryATo(request.getBatteryATo())
                            .batteryCells(request.getBatteryCells())
                            .batteryModules(request.getBatteryModules())
                            .batteryPacks(request.getBatteryPacks())
                            .chargerLevel(request.getChargerLevel())
                            .chargerPowerKW(request.getChargerPowerKW())
                            .adaptiveCruiseControl(request.getAdaptiveCruiseControl())
                            .adaptiveDrivingBeam(request.getAdaptiveDrivingBeam())
                            .adaptiveHeadlights(request.getAdaptiveHeadlights())
                            .keylessIgnition(request.getKeylessIgnition())
                            .wheelieMitigation(request.getWheelieMitigation())
                            .automaticPedestrianAlertingSound(
                                    request.getAutomaticPedestrianAlertingSound())
                            .autoReverseSystem(request.getAutoReverseSystem())
                            .cibStatus(request.getCibStatus())
                            .daytimeRunningLight(request.getDaytimeRunningLight())
                            .lowerBeamHeadlampLightSource(request.getLowerBeamHeadlampLightSource())
                            .semiautomaticHeadlampBeamSwitching(
                                    request.getSemiautomaticHeadlampBeamSwitching())
                            .plantCity(request.getPlantCity())
                            .plantState(request.getPlantState())
                            .plantCountry(request.getPlantCountry())
                            .destinationMarket(request.getDestinationMarket())
                            .saeAutomationLevel(request.getSaeAutomationLevel())
                            .saeAutomationLevelTo(request.getSaeAutomationLevelTo())
                            .crac(request.getCrac())
                            .steeringLocation(request.getSteeringLocation())
                            .basePrice(request.getBasePrice())
                            .cashForClunkers(request.getCashForClunkers())
                            .ncsbBodyType(request.getNcsbBodyType())
                            .ncsaMake(request.getNcsaMake())
                            .ncsaModel(request.getNcsaModel())
                            .ncsbMappingException(request.getNcsbMappingException())
                            .ncsbMapExcApprovedBy(request.getNcsbMapExcApprovedBy())
                            .ncsbMapExcApprovedOn(request.getNcsbMapExcApprovedOn())
                            .ncsbNote(request.getNcsbNote())
                            .vehicleDescriptor(request.getVehicleDescriptor())
                            .suggestedVin(request.getSuggestedVin())
                            .possibleValues(request.getPossibleValues())
                            .note(request.getNote())
                            .attributes(request.getAttributes())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

            unitRepository.save(entity);
            log.info("Created unit: {}", unitId);

            return entity;
        }
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

            // Check for duplicate VIN
            final List<Unit> existing = unitRepository.findByVin(vin);
            if (!existing.isEmpty()) {
                log.warn("Duplicate VIN detected: {}", vin);
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
     * Get a Unit by VIN.
     *
     * @param vin The VIN
     * @return The unit DTO or null if not found
     */
    public Unit getUnitByVin(String vin) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-service-getUnitByVin")) {
            segment.putAnnotation("vin", vin);

            final List<Unit> entities = unitRepository.findByVin(vin);
            if (entities.isEmpty()) {
                log.debug("No unit found for VIN: {}", vin);
                return null;
            }

            log.debug("Retrieved unit by VIN: {}", vin);
            return entities.get(0);
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

            // If VIN is being updated, check for duplicates
            if (request.getVin() != null && !request.getVin().equals(entity.vin())) {
                final List<Unit> existing = unitRepository.findByVin(request.getVin());
                if (!existing.isEmpty()) {
                    log.warn("Duplicate VIN detected during update: {}", request.getVin());
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
