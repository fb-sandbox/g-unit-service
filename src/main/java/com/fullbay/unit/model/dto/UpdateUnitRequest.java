package com.fullbay.unit.model.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/** Request DTO for updating an existing Unit. Only association fields are updatable. Immutable. */
@Value
@Builder
public class UpdateUnitRequest {

    // Association fields
    String customerId;
    String vin;

    // Flexible Storage
    Map<String, Object> attributes;
}
