package com.fullbay.unit.model.response;

import lombok.Builder;
import lombok.Value;

/** Individual validation error detail. Immutable. */
@Value
@Builder
public class ValidationError {

    String field;
    String message;
}
