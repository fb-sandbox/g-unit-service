package com.fullbay.unit.integration.nhtsa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Response DTO from NHTSA VIN decode API. */
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NHTSAVinDecodeResponse {

    @JsonProperty("Count")
    Integer count;

    @JsonProperty("Message")
    String message;

    @JsonProperty("SearchCriteria")
    String searchType;

    Integer variableCount;

    @JsonProperty("Results")
    List<NHTSAResult> results;

    /**
     * Get a result value by variable name.
     *
     * @param variableName The NHTSA variable name
     * @return The value or null if not found
     */
    public String getResultValue(String variableName) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.stream()
                .filter(r -> r != null && variableName.equals(r.getVariable()))
                .map(NHTSAResult::getValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a result value as Integer.
     *
     * @param variableName The NHTSA variable name
     * @return The integer value or null if not found or not a valid integer
     */
    public Integer getResultValueAsInt(String variableName) {
        final String value = getResultValue(variableName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get a result value as Double.
     *
     * @param variableName The NHTSA variable name
     * @return The double value or null if not found or not a valid double
     */
    public Double getResultValueAsDouble(String variableName) {
        final String value = getResultValue(variableName);
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
