package com.fullbay.unit.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.Value;

/** Request DTO for creating a new Unit from VIN via NHTSA API. Immutable. */
@Value
@Builder
public class CreateUnitFromVinRequest {

    @NotNull(message = "customerId is required")
    String customerId;

    @NotNull(message = "vin is required")
    @Size(min = 5, max = 50, message = "vin must be between 5 and 50 characters")
    String vin;
}
