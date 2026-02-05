package com.fullbay.unit.exception;

/** Exception thrown when a duplicate VIN is detected. */
public class DuplicateVinException extends RuntimeException {

    public DuplicateVinException(String vin) {
        super("Duplicate VIN: " + vin);
    }
}
