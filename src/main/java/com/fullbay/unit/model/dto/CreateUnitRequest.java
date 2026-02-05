package com.fullbay.unit.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/** Request DTO for creating a new Unit. Immutable. */
@Value
@Builder
public class CreateUnitRequest {

    // Required Fields
    @NotNull(message = "customerId is required")
    String customerId;

    @NotNull(message = "vin is required")
    @Size(min = 5, max = 50, message = "vin must be between 5 and 50 characters")
    String vin;

    // Vehicle Identification
    Integer year;
    String make;
    Integer makeId;
    String manufacturer;
    Integer manufacturerId;
    String model;
    Integer modelId;
    String series;
    String trim;
    String trim2;
    String submodel;

    // Vehicle Classification
    String unitType;
    String vehicleType;
    String bodyClass;
    String bodyType;
    String bodyCabType;
    String bedType;
    String busType;
    String busLength;
    String busFloorConfigType;
    String motorcycleChassisType;
    String motorcycleSuspensionType;
    String trailerBodyType;
    String trailerLength;
    String trailerType;
    String customMotorcycleType;
    String nonLandUse;
    String otherBusInfo;
    String otherMotorcycleInfo;
    String otherTrailerInfo;

    // Engine Specifications
    String fuelType;
    String fuelTypeSecondary;
    String engineType;
    String engineManufacturer;
    String engineModel;
    Integer engineCylinders;
    Integer engineHP;
    Integer engineHPMax;
    Integer engineKW;
    Double displacementLiters;
    Double displacementCC;
    Double displacementCI;
    String engineConfiguration;
    String engineCycles;
    String valveTrainDesign;
    String fuelInjectionType;
    String otherEngineInfo;
    String turbo;
    String coolingType;
    String topSpeedMPH;

    // Transmission & Drivetrain
    String transmissionType;
    String transmissionStyle;
    String transmissionSpeeds;
    String driveType;
    String brakeSystemType;
    String brakeSystemDesc;
    String combinedBrakingSystem;
    String dynamicBrakeSupport;
    Integer axles;
    String axleConfiguration;

    // Dimensions & Weight
    Integer doors;
    Integer windows;
    Integer seats;
    Integer seatRows;
    Integer curbWeightLB;
    String gvwr;
    String gvwrTo;
    Integer gcwr;
    Integer gcwrTo;
    Integer bedLengthIN;
    Integer wheelbaseIN;
    Integer wheelbaseShort;
    Integer wheelbaseLong;
    String wheelbaseType;
    Integer trackWidth;
    Integer wheelSizeFront;
    Integer wheelSizeRear;

    // Safety Systems
    String abs;
    String esc;
    String tractionControl;
    String forwardCollisionWarning;
    String blindSpotMon;
    String blindSpotIntervention;
    String laneDepartureWarning;
    String laneKeepSystem;
    String laneCenteringAssistance;
    String parkAssist;
    String rearCrossTrafficAlert;
    String rearAutomaticEmergencyBraking;
    String rearVisibilitySystem;
    String pedestrianAutomaticEmergencyBraking;
    String seatBelts;
    String seatBeltsAll;
    String pretensioner;
    String airBagsFront;
    String airBagsKnee;
    String airBagsSide;
    String airBagsCurtain;
    String airBagsSeatCushion;
    String airbagLocFront;
    String airbagLocKnee;
    String airbagLocSide;
    String airbagLocCurtain;
    String airbagLocSeatCushion;
    String activeSafetyNote;
    String activeSafetySysNote;
    String otherRestraintSystemInfo;
    String cib;
    String edr;

    // EV/Battery Systems
    String batteryType;
    String batteryInfo;
    String evDriveUnit;
    String electrificationLevel;
    Double batteryKWh;
    Double batteryKWhTo;
    Integer batteryV;
    Integer batteryVTo;
    Integer batteryA;
    Integer batteryATo;
    Integer batteryCells;
    Integer batteryModules;
    Integer batteryPacks;
    String chargerLevel;
    Integer chargerPowerKW;

    // Adaptive Features
    String adaptiveCruiseControl;
    String adaptiveDrivingBeam;
    String adaptiveHeadlights;
    String keylessIgnition;
    String wheelieMitigation;
    String automaticPedestrianAlertingSound;
    String autoReverseSystem;
    String cibStatus;
    String daytimeRunningLight;
    String lowerBeamHeadlampLightSource;
    String semiautomaticHeadlampBeamSwitching;

    // Manufacturing Info
    String plantCity;
    String plantState;
    String plantCountry;
    String destinationMarket;

    // SAE/Automation
    String saeAutomationLevel;
    String saeAutomationLevelTo;
    String crac;

    // Other NHTSA Fields
    String steeringLocation;
    String basePrice;
    String cashForClunkers;
    String ncsbBodyType;
    String ncsaMake;
    String ncsaModel;
    String ncsbMappingException;
    String ncsbMapExcApprovedBy;
    String ncsbMapExcApprovedOn;
    String ncsbNote;
    String vehicleDescriptor;
    String suggestedVin;
    String possibleValues;
    String note;

    // Flexible Storage
    Map<String, Object> attributes;
}
