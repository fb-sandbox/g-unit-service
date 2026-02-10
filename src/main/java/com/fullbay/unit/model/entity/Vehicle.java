package com.fullbay.unit.model.entity;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/** Vehicle data decoded from NHTSA VIN API. Stored once per VIN as VIN# items in DynamoDB. */
@Builder
@Value
@Accessors(fluent = true)
@Jacksonized
@Wither
public class Vehicle {

    // Identifier
    String vin;

    // Vehicle Identification (NHTSA Basic Info)
    Integer year;
    String make;
    Integer makeId;
    String manufacturer;
    Integer manufacturerId;
    String model;
    Integer modelId;
    Integer baseVehicleId;
    Integer engineBaseId;
    String series;
    String trim;
    String trim2;
    String submodel;

    // Vehicle Classification (NHTSA Body Type)
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

    // Engine Specifications (NHTSA Engine Data)
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

    // Transmission & Drivetrain (NHTSA Transmission & Axle)
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

    // Dimensions & Weight (NHTSA Physical Specs)
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

    // Safety Systems (NHTSA Safety)
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

    // EV/Battery Systems (NHTSA EV Data)
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

    // Adaptive Features (NHTSA Advanced Systems)
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

    // Manufacturing Info (NHTSA Plant Data)
    String plantCity;
    String plantState;
    String plantCountry;
    String destinationMarket;

    // SAE/Automation (NHTSA Autonomous Systems)
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

    // Timestamps
    Instant createdAt;
    Instant updatedAt;
}
