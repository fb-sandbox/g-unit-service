package com.fullbay.unit.exception;

/** Exception thrown when a Unit is not found. */
public class UnitNotFoundException extends RuntimeException {

    public UnitNotFoundException(String unitId) {
        super("Unit not found: " + unitId);
    }
}
