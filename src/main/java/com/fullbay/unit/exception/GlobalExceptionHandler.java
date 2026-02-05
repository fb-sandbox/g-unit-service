package com.fullbay.unit.exception;

import com.fullbay.unit.model.response.ApiResponse;
import com.fullbay.unit.model.response.ErrorDetail;
import com.fullbay.unit.model.response.ValidationError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/** Global exception handler for REST endpoints. */
@Provider
@Slf4j
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        log.error("Exception occurred", exception);

        if (exception instanceof UnitNotFoundException) {
            return handleUnitNotFound((UnitNotFoundException) exception);
        }

        if (exception instanceof DuplicateVinException) {
            return handleDuplicateVin((DuplicateVinException) exception);
        }

        if (exception instanceof ConstraintViolationException) {
            return handleValidationError((ConstraintViolationException) exception);
        }

        return handleGenericError(exception);
    }

    private Response handleUnitNotFound(UnitNotFoundException exception) {
        final ErrorDetail error =
                ErrorDetail.builder()
                        .code("UNIT_NOT_FOUND")
                        .message(exception.getMessage())
                        .build();

        final ApiResponse<Void> response = ApiResponse.<Void>builder().error(error).build();

        return Response.status(Response.Status.NOT_FOUND)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleDuplicateVin(DuplicateVinException exception) {
        final ErrorDetail error =
                ErrorDetail.builder().code("DUPLICATE_VIN").message(exception.getMessage()).build();

        final ApiResponse<Void> response = ApiResponse.<Void>builder().error(error).build();

        return Response.status(Response.Status.CONFLICT)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleValidationError(ConstraintViolationException exception) {
        final List<ValidationError> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            details.add(
                    ValidationError.builder()
                            .field(violation.getPropertyPath().toString())
                            .message(violation.getMessage())
                            .build());
        }

        final ErrorDetail error =
                ErrorDetail.builder()
                        .code("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .details(details)
                        .build();

        final ApiResponse<Void> response = ApiResponse.<Void>builder().error(error).build();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleGenericError(Exception exception) {
        final ErrorDetail error =
                ErrorDetail.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message(
                                exception.getMessage() != null
                                        ? exception.getMessage()
                                        : "An unexpected error occurred")
                        .build();

        final ApiResponse<Void> response = ApiResponse.<Void>builder().error(error).build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
