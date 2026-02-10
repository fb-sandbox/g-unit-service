package com.fullbay.unit.model.dto;

import lombok.Builder;
import lombok.Value;

/** Response DTO for an aftermarket part from ACES fitment data. Immutable. */
@Value
@Builder
public class PartDto {

    String partNumber;
    String brandName;
    String category;
    String partType;
    String position;
    Integer quantity;
    String note;
}
