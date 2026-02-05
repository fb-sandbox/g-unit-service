package com.fullbay.unit.integration.nhtsa;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/** Single result object from NHTSA VIN decode API response. */
@Value
@Builder
@AllArgsConstructor
public class NHTSAResult {

    @JsonProperty("Value")
    String value;

    @JsonProperty("ValueId")
    String valueId;

    @JsonProperty("Variable")
    String variable;

    @JsonProperty("VariableId")
    Integer variableId;
}
