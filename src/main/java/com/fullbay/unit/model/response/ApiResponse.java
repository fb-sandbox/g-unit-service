package com.fullbay.unit.model.response;

import lombok.Builder;
import lombok.Value;

/** Generic API response wrapper. Immutable. */
@Value
@Builder
public class ApiResponse<T> {

    T data;
    ErrorDetail error;
}
