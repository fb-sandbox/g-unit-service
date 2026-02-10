package com.fullbay.unit.integration.parts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Model entity from parts-service. */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartsModel {

    String modelId;
    String modelName;
    String vehicleTypeId;
    String vehicleTypeName;
}
