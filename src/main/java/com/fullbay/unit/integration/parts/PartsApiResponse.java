package com.fullbay.unit.integration.parts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Generic API response wrapper matching parts-service format. */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartsApiResponse<T> {

    T data;
}
