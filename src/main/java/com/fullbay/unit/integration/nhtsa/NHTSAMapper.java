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
                .year(parseInteger(response.getResultValue("Model Year")))
                .make(response.getResultValue("Make"))
                .manufacturer(response.getResultValue("Manufacturer Name"))
                .model(response.getResultValue("Model"))
                .series(response.getResultValue("Series"))
                .trim(response.getResultValue("Trim"))
                .trim2(response.getResultValue("Trim2"))
                // Vehicle Classification
                .unitType(response.getResultValue("Vehicle Type"))
                .vehicleType(response.getResultValue("Vehicle Type"))
                .bodyClass(response.getResultValue("Body Class"))
                .bodyType(response.getResultValue("Cab Type"))
                .bodyCabType(response.getResultValue("Cab Type"))
                .bedType(response.getResultValue("Bed Type"))
                .busType(response.getResultValue("Bus Type"))
                .busLength(response.getResultValue("Bus Length (feet)"))
                .busFloorConfigType(response.getResultValue("Bus Floor Configuration Type"))
                .motorcycleChassisType(response.getResultValue("Motorcycle Chassis Type"))
                .motorcycleSuspensionType(response.getResultValue("Motorcycle Suspension Type"))
                .trailerBodyType(response.getResultValue("Trailer Body Type"))
                .trailerLength(response.getResultValue("Trailer Length (feet)"))
                .trailerType(response.getResultValue("Trailer Type Connection"))
                .customMotorcycleType(response.getResultValue("Custom Motorcycle Type"))
                .nonLandUse(response.getResultValue("Non-Land Use"))
                .otherBusInfo(response.getResultValue("Other Bus Info"))
                .otherMotorcycleInfo(response.getResultValue("Other Motorcycle Info"))
                .otherTrailerInfo(response.getResultValue("Other Trailer Info"))
                // Engine Specifications
                .fuelType(response.getResultValue("Fuel Type - Primary"))
                .fuelTypeSecondary(response.getResultValue("Fuel Type - Secondary"))
                .engineType(response.getResultValue("Fuel Type - Primary"))
                .engineManufacturer(response.getResultValue("Engine Manufacturer"))
                .engineModel(response.getResultValue("Engine Model"))
                .engineCylinders(
                        parseInteger(response.getResultValue("Engine Number of Cylinders")))
                .engineHP(parseInteger(response.getResultValue("Engine Brake (hp) From")))
                .engineHPMax(parseInteger(response.getResultValue("Engine Brake (hp) To")))
                .engineKW(parseInteger(response.getResultValue("Engine Power (kW)")))
                .displacementLiters(response.getResultValueAsDouble("Displacement (L)"))
                .displacementCC(response.getResultValueAsDouble("Displacement (CC)"))
                .displacementCI(response.getResultValueAsDouble("Displacement (CI)"))
                .engineConfiguration(response.getResultValue("Engine Configuration"))
                .engineCycles(response.getResultValue("Engine Stroke Cycles"))
                .valveTrainDesign(response.getResultValue("Valve Train Design"))
                .fuelInjectionType(response.getResultValue("Fuel Delivery / Fuel Injection Type"))
                .otherEngineInfo(response.getResultValue("Other Engine Info"))
                .turbo(response.getResultValue("Turbo"))
                .coolingType(response.getResultValue("Cooling Type"))
                .topSpeedMPH(response.getResultValue("Top Speed (MPH)"))
                // Transmission & Drivetrain
                .transmissionType(response.getResultValue("Transmission Style"))
                .transmissionStyle(response.getResultValue("Transmission Style"))
                .transmissionSpeeds(response.getResultValue("Transmission Speeds"))
                .driveType(response.getResultValue("Drive Type"))
                .brakeSystemType(response.getResultValue("Brake System Type"))
                .brakeSystemDesc(response.getResultValue("Brake System Description"))
                .combinedBrakingSystem(response.getResultValue("Combined Braking System (CBS)"))
                .dynamicBrakeSupport(response.getResultValue("Dynamic Brake Support (DBS)"))
                .axles(parseInteger(response.getResultValue("Axles")))
                .axleConfiguration(response.getResultValue("Axle Configuration"))
                // Dimensions & Weight
                .doors(parseInteger(response.getResultValue("Doors")))
                .windows(parseInteger(response.getResultValue("Windows")))
                .seats(parseInteger(response.getResultValue("Number of Seats")))
                .seatRows(parseInteger(response.getResultValue("Number of Seat Rows")))
                .curbWeightLB(parseInteger(response.getResultValue("Curb Weight (pounds)")))
                .gvwr(response.getResultValue("Gross Vehicle Weight Rating From"))
                .gvwrTo(response.getResultValue("Gross Vehicle Weight Rating To"))
                .gcwr(parseInteger(response.getResultValue("Gross Combination Weight Rating From")))
                .gcwrTo(parseInteger(response.getResultValue("Gross Combination Weight Rating To")))
                .bedLengthIN(parseInteger(response.getResultValue("Bed Length (inches)")))
                .wheelbaseIN(parseInteger(response.getResultValue("Wheel Base (inches) From")))
                .wheelbaseLong(parseInteger(response.getResultValue("Wheel Base (inches) To")))
                .wheelbaseType(response.getResultValue("Wheel Base Type"))
                .trackWidth(parseInteger(response.getResultValue("Track Width (inches)")))
                .wheelSizeFront(parseInteger(response.getResultValue("Wheel Size Front (inches)")))
                .wheelSizeRear(parseInteger(response.getResultValue("Wheel Size Rear (inches)")))
                // Safety Systems
                .abs(response.getResultValue("Anti-lock Braking System (ABS)"))
                .esc(response.getResultValue("Electronic Stability Control (ESC)"))
                .tractionControl(response.getResultValue("Traction Control"))
                .forwardCollisionWarning(response.getResultValue("Forward Collision Warning (FCW)"))
                .blindSpotMon(response.getResultValue("Blind Spot Warning (BSW)"))
                .blindSpotIntervention(response.getResultValue("Blind Spot Intervention (BSI)"))
                .laneDepartureWarning(response.getResultValue("Lane Departure Warning (LDW)"))
                .laneKeepSystem(response.getResultValue("Lane Keeping Assistance (LKA)"))
                .laneCenteringAssistance(response.getResultValue("Lane Centering Assistance"))
                .parkAssist(response.getResultValue("Parking Assist"))
                .rearCrossTrafficAlert(response.getResultValue("Rear Cross Traffic Alert"))
                .rearAutomaticEmergencyBraking(
                        response.getResultValue("Rear Automatic Emergency Braking"))
                .pedestrianAutomaticEmergencyBraking(
                        response.getResultValue("Pedestrian Automatic Emergency Braking (PAEB)"))
                .seatBelts(response.getResultValue("Seat Belt Type"))
                .seatBeltsAll(response.getResultValue("Seat Belt Type"))
                .pretensioner(response.getResultValue("Pretensioner"))
                .airBagsFront(response.getResultValue("Front Air Bag Locations"))
                .airBagsKnee(response.getResultValue("Knee Air Bag Locations"))
                .airBagsSide(response.getResultValue("Side Air Bag Locations"))
                .airBagsCurtain(response.getResultValue("Curtain Air Bag Locations"))
                .airBagsSeatCushion(response.getResultValue("Seat Cushion Air Bag Locations"))
                .airbagLocFront(response.getResultValue("Front Air Bag Locations"))
                .airbagLocKnee(response.getResultValue("Knee Air Bag Locations"))
                .airbagLocSide(response.getResultValue("Side Air Bag Locations"))
                .airbagLocCurtain(response.getResultValue("Curtain Air Bag Locations"))
                .airbagLocSeatCushion(response.getResultValue("Seat Cushion Air Bag Locations"))
                .activeSafetyNote(response.getResultValue("Active Safety System Note"))
                .activeSafetySysNote(response.getResultValue("Active Safety System Note"))
                .otherRestraintSystemInfo(response.getResultValue("Other Restraint System Info"))
                .cib(response.getResultValue("Crash Imminent Braking (CIB)"))
                .edr(response.getResultValue("Event Data Recorder (EDR)"))
                // EV/Battery Systems
                .batteryType(response.getResultValue("Battery Type"))
                .batteryInfo(response.getResultValue("Other Battery Info"))
                .evDriveUnit(response.getResultValue("EV Drive Unit"))
                .electrificationLevel(response.getResultValue("Electrification Level"))
                .batteryKWh(response.getResultValueAsDouble("Battery Energy (kWh) From"))
                .batteryKWhTo(response.getResultValueAsDouble("Battery Energy (kWh) To"))
                .batteryV(parseInteger(response.getResultValue("Battery Voltage (Volts) From")))
                .batteryVTo(parseInteger(response.getResultValue("Battery Voltage (Volts) To")))
                .batteryA(parseInteger(response.getResultValue("Battery Current (Amps) From")))
                .batteryATo(parseInteger(response.getResultValue("Battery Current (Amps) To")))
                .batteryCells(
                        parseInteger(response.getResultValue("Number of Battery Cells per Module")))
                .batteryModules(
                        parseInteger(response.getResultValue("Number of Battery Modules per Pack")))
                .batteryPacks(
                        parseInteger(
                                response.getResultValue("Number of Battery Packs per Vehicle")))
                .chargerLevel(response.getResultValue("Charger Level"))
                .chargerPowerKW(parseInteger(response.getResultValue("Charger Power (kW)")))
                // Adaptive Features
                .adaptiveCruiseControl(response.getResultValue("Adaptive Cruise Control (ACC)"))
                .adaptiveDrivingBeam(response.getResultValue("Adaptive Driving Beam (ADB)"))
                .keylessIgnition(response.getResultValue("Keyless Ignition"))
                .wheelieMitigation(response.getResultValue("Wheelie Mitigation"))
                .automaticPedestrianAlertingSound(
                        response.getResultValue(
                                "Automatic Pedestrian Alerting Sound (for Hybrid and EV only)"))
                .autoReverseSystem(
                        response.getResultValue("Auto-Reverse System for Windows and Sunroofs"))
                .daytimeRunningLight(response.getResultValue("Daytime Running Light (DRL)"))
                .lowerBeamHeadlampLightSource(response.getResultValue("Headlamp Light Source"))
                .semiautomaticHeadlampBeamSwitching(
                        response.getResultValue("Semiautomatic Headlamp Beam Switching"))
                // Manufacturing Info
                .plantCity(response.getResultValue("Plant City"))
                .plantState(response.getResultValue("Plant State"))
                .plantCountry(response.getResultValue("Plant Country"))
                // SAE/Automation
                .saeAutomationLevel(response.getResultValue("SAE Automation Level From"))
                .saeAutomationLevelTo(response.getResultValue("SAE Automation Level To"))
                // Other NHTSA Fields
                .steeringLocation(response.getResultValue("Steering Location"))
                .basePrice(response.getResultValue("Base Price ($)"))
                .vehicleDescriptor(response.getResultValue("Vehicle Descriptor"))
                .suggestedVin(response.getResultValue("Suggested VIN"))
                .possibleValues(response.getResultValue("Possible Values"))
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
