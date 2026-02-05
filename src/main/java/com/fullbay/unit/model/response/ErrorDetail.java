package com.fullbay.unit.model.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** Error response detail. Immutable. */
@Value
@Builder
public class ErrorDetail {

    String code;
    String message;
    List<ValidationError> details;
}
