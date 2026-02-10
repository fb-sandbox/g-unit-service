package com.fullbay.unit.integration.parts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Vehicle entity from parts-service. */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartsVehicle {

    String baseVehicleId;
    String yearId;
    String makeId;
    String makeName;
    String modelId;
    String modelName;
    String vehicleTypeId;
    String vehicleTypeName;
}
