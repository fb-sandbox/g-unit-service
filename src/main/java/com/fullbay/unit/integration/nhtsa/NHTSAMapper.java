package com.fullbay.unit.integration.nhtsa;

import com.fullbay.unit.model.entity.Unit;

import java.time.Instant;

/** Mapper for converting NHTSA VIN decode response to Unit. Static utility class. */
public final class NHTSAMapper {

    private NHTSAMapper() {
        // Utility class
    }

    /**
     * Convert NHTSA VIN decode response to Unit.
     *
     * @param response The NHTSA response
     * @param vin The VIN
     * @param customerId The customer ID
     * @param unitId The generated unit ID
     * @return The entity
     */
    public static Unit toEntity(
            NHTSAVinDecodeResponse response, String vin, String customerId, String unitId) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }

        final Instant now = Instant.now();
        return Unit.builder()
                .unitId(unitId)
                .customerId(customerId)
                .vin(vin)
                // Vehicle Identification
                .year(parseInteger(response.getResultValue("ModelYear")))
                .make(response.getResultValue("Make"))
                .makeId(parseInteger(response.getResultValue("MakeID")))
                .manufacturer(response.getResultValue("Manufacturer"))
                .manufacturerId(parseInteger(response.getResultValue("ManufacturerId")))
                .model(response.getResultValue("Model"))
                .modelId(parseInteger(response.getResultValue("ModelID")))
                .series(response.getResultValue("Series"))
                .trim(response.getResultValue("Trim"))
                .trim2(response.getResultValue("Trim2"))
                .submodel(response.getResultValue("Submodel"))
                // Vehicle Classification
                .unitType(response.getResultValue("VehicleType"))
                .vehicleType(response.getResultValue("VehicleType"))
                .bodyClass(response.getResultValue("BodyClass"))
                .bodyType(response.getResultValue("BodyCabType"))
                .bodyCabType(response.getResultValue("BodyCabType"))
                .bedType(response.getResultValue("BedType"))
                .busType(response.getResultValue("BusType"))
                .busLength(response.getResultValue("BusLength"))
                .busFloorConfigType(response.getResultValue("BusFloorConfigType"))
                .motorcycleChassisType(response.getResultValue("MotorcycleChassisType"))
                .motorcycleSuspensionType(response.getResultValue("MotorcycleSuspensionType"))
                .trailerBodyType(response.getResultValue("TrailerBodyType"))
                .trailerLength(response.getResultValue("TrailerLength"))
                .trailerType(response.getResultValue("TrailerType"))
                .customMotorcycleType(response.getResultValue("CustomMotorcycleType"))
                .nonLandUse(response.getResultValue("NonLandUse"))
                .otherBusInfo(response.getResultValue("OtherBusInfo"))
                .otherMotorcycleInfo(response.getResultValue("OtherMotorcycleInfo"))
                .otherTrailerInfo(response.getResultValue("OtherTrailerInfo"))
                // Engine Specifications
                .fuelType(response.getResultValue("FuelTypePrimary"))
                .fuelTypeSecondary(response.getResultValue("FuelTypeSecondary"))
                .engineType(response.getResultValue("FuelTypePrimary"))
                .engineManufacturer(response.getResultValue("EngineManufacturer"))
                .engineModel(response.getResultValue("EngineModel"))
                .engineCylinders(parseInteger(response.getResultValue("EngineCylinders")))
                .engineHP(parseInteger(response.getResultValue("EngineHP")))
                .engineHPMax(parseInteger(response.getResultValue("EngineHP_to")))
                .engineKW(parseInteger(response.getResultValue("EngineKW")))
                .displacementLiters(response.getResultValueAsDouble("DisplacementL"))
                .displacementCC(response.getResultValueAsDouble("DisplacementCC"))
                .displacementCI(response.getResultValueAsDouble("DisplacementCI"))
                .engineConfiguration(response.getResultValue("EngineConfiguration"))
                .engineCycles(response.getResultValue("EngineCycles"))
                .valveTrainDesign(response.getResultValue("ValveTrainDesign"))
                .fuelInjectionType(response.getResultValue("FuelInjectionType"))
                .otherEngineInfo(response.getResultValue("OtherEngineInfo"))
                .turbo(response.getResultValue("Turbo"))
                .coolingType(response.getResultValue("CoolingType"))
                .topSpeedMPH(response.getResultValue("TopSpeedMPH"))
                // Transmission & Drivetrain
                .transmissionType(response.getResultValue("TransmissionStyle"))
                .transmissionStyle(response.getResultValue("TransmissionStyle"))
                .transmissionSpeeds(response.getResultValue("TransmissionSpeeds"))
                .driveType(response.getResultValue("DriveType"))
                .brakeSystemType(response.getResultValue("BrakeSystemType"))
                .brakeSystemDesc(response.getResultValue("BrakeSystemDesc"))
                .combinedBrakingSystem(response.getResultValue("CombinedBrakingSystem"))
                .dynamicBrakeSupport(response.getResultValue("DynamicBrakeSupport"))
                .axles(parseInteger(response.getResultValue("Axles")))
                .axleConfiguration(response.getResultValue("AxleConfiguration"))
                // Dimensions & Weight
                .doors(parseInteger(response.getResultValue("Doors")))
                .windows(parseInteger(response.getResultValue("Windows")))
                .seats(parseInteger(response.getResultValue("Seats")))
                .seatRows(parseInteger(response.getResultValue("SeatRows")))
                .curbWeightLB(parseInteger(response.getResultValue("CurbWeightLB")))
                .gvwr(response.getResultValue("GVWR"))
                .gvwrTo(response.getResultValue("GVWR_to"))
                .gcwr(parseInteger(response.getResultValue("GCWR")))
                .gcwrTo(parseInteger(response.getResultValue("GCWR_to")))
                .bedLengthIN(parseInteger(response.getResultValue("BedLengthIN")))
                .wheelbaseIN(parseInteger(response.getResultValue("WheelBase")))
                .wheelbaseShort(parseInteger(response.getResultValue("WheelBaseShort")))
                .wheelbaseLong(parseInteger(response.getResultValue("WheelBaseLong")))
                .wheelbaseType(response.getResultValue("WheelBaseType"))
                .trackWidth(parseInteger(response.getResultValue("TrackWidth")))
                .wheelSizeFront(parseInteger(response.getResultValue("WheelSizeFront")))
                .wheelSizeRear(parseInteger(response.getResultValue("WheelSizeRear")))
                // Safety Systems
                .abs(response.getResultValue("ABS"))
                .esc(response.getResultValue("ESC"))
                .tractionControl(response.getResultValue("TractionControl"))
                .forwardCollisionWarning(response.getResultValue("ForwardCollisionWarning"))
                .blindSpotMon(response.getResultValue("BlindSpotMon"))
                .blindSpotIntervention(response.getResultValue("BlindSpotIntervention"))
                .laneDepartureWarning(response.getResultValue("LaneDepartureWarning"))
                .laneKeepSystem(response.getResultValue("LaneKeepSystem"))
                .laneCenteringAssistance(response.getResultValue("LaneCenteringAssistance"))
                .parkAssist(response.getResultValue("ParkAssist"))
                .rearCrossTrafficAlert(response.getResultValue("RearCrossTrafficAlert"))
                .rearAutomaticEmergencyBraking(
                        response.getResultValue("RearAutomaticEmergencyBraking"))
                .rearVisibilitySystem(response.getResultValue("RearVisibilitySystem"))
                .pedestrianAutomaticEmergencyBraking(
                        response.getResultValue("PedestrianAutomaticEmergencyBraking"))
                .seatBelts(response.getResultValue("SeatBeltsAll"))
                .seatBeltsAll(response.getResultValue("SeatBeltsAll"))
                .pretensioner(response.getResultValue("Pretensioner"))
                .airBagsFront(response.getResultValue("AirBagLocFront"))
                .airBagsKnee(response.getResultValue("AirBagLocKnee"))
                .airBagsSide(response.getResultValue("AirBagLocSide"))
                .airBagsCurtain(response.getResultValue("AirBagLocCurtain"))
                .airBagsSeatCushion(response.getResultValue("AirBagLocSeatCushion"))
                .airbagLocFront(response.getResultValue("AirBagLocFront"))
                .airbagLocKnee(response.getResultValue("AirBagLocKnee"))
                .airbagLocSide(response.getResultValue("AirBagLocSide"))
                .airbagLocCurtain(response.getResultValue("AirBagLocCurtain"))
                .airbagLocSeatCushion(response.getResultValue("AirBagLocSeatCushion"))
                .activeSafetyNote(response.getResultValue("ActiveSafetyNote"))
                .activeSafetySysNote(response.getResultValue("ActiveSafetySysNote"))
                .otherRestraintSystemInfo(response.getResultValue("OtherRestraintSystemInfo"))
                .cib(response.getResultValue("CIB"))
                .edr(response.getResultValue("EDR"))
                // EV/Battery Systems
                .batteryType(response.getResultValue("BatteryType"))
                .batteryInfo(response.getResultValue("BatteryInfo"))
                .evDriveUnit(response.getResultValue("EVDriveUnit"))
                .electrificationLevel(response.getResultValue("ElectrificationLevel"))
                .batteryKWh(response.getResultValueAsDouble("BatteryKWh"))
                .batteryKWhTo(response.getResultValueAsDouble("BatteryKWh_to"))
                .batteryV(parseInteger(response.getResultValue("BatteryV")))
                .batteryVTo(parseInteger(response.getResultValue("BatteryV_to")))
                .batteryA(parseInteger(response.getResultValue("BatteryA")))
                .batteryATo(parseInteger(response.getResultValue("BatteryA_to")))
                .batteryCells(parseInteger(response.getResultValue("BatteryCells")))
                .batteryModules(parseInteger(response.getResultValue("BatteryModules")))
                .batteryPacks(parseInteger(response.getResultValue("BatteryPacks")))
                .chargerLevel(response.getResultValue("ChargerLevel"))
                .chargerPowerKW(parseInteger(response.getResultValue("ChargerPowerKW")))
                // Adaptive Features
                .adaptiveCruiseControl(response.getResultValue("AdaptiveCruiseControl"))
                .adaptiveDrivingBeam(response.getResultValue("AdaptiveDrivingBeam"))
                .adaptiveHeadlights(response.getResultValue("AdaptiveHeadlights"))
                .keylessIgnition(response.getResultValue("KeylessIgnition"))
                .wheelieMitigation(response.getResultValue("WheelieMitigation"))
                .automaticPedestrianAlertingSound(
                        response.getResultValue("AutomaticPedestrianAlertingSound"))
                .autoReverseSystem(response.getResultValue("AutoReverseSystem"))
                .daytimeRunningLight(response.getResultValue("DaytimeRunningLight"))
                .lowerBeamHeadlampLightSource(
                        response.getResultValue("LowerBeamHeadlampLightSource"))
                .semiautomaticHeadlampBeamSwitching(
                        response.getResultValue("SemiautomaticHeadlampBeamSwitching"))
                // Manufacturing Info
                .plantCity(response.getResultValue("PlantCity"))
                .plantState(response.getResultValue("PlantState"))
                .plantCountry(response.getResultValue("PlantCountry"))
                .destinationMarket(response.getResultValue("DestinationMarket"))
                // SAE/Automation
                .saeAutomationLevel(response.getResultValue("SAEAutomationLevel"))
                .saeAutomationLevelTo(response.getResultValue("SAEAutomationLevel_to"))
                // Other NHTSA Fields
                .steeringLocation(response.getResultValue("SteeringLocation"))
                .basePrice(response.getResultValue("BasePrice"))
                .cashForClunkers(response.getResultValue("CashForClunkers"))
                .ncsbBodyType(response.getResultValue("NCSABodyType"))
                .ncsaMake(response.getResultValue("NCSAMake"))
                .ncsaModel(response.getResultValue("NCSAModel"))
                .ncsbMappingException(response.getResultValue("NCSAMappingException"))
                .ncsbMapExcApprovedBy(response.getResultValue("NCSAMapExcApprovedBy"))
                .ncsbMapExcApprovedOn(response.getResultValue("NCSAMapExcApprovedOn"))
                .ncsbNote(response.getResultValue("NCSANote"))
                .vehicleDescriptor(response.getResultValue("VehicleDescriptor"))
                .suggestedVin(response.getResultValue("SuggestedVIN"))
                .possibleValues(response.getResultValue("PossibleValues"))
                .note(response.getResultValue("Note"))
                // Timestamps
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Parse a string to Integer, returning null if parsing fails.
     *
     * @param value The string value
     * @return The integer or null
     */
    private static Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
