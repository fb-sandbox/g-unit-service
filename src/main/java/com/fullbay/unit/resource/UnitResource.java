package com.fullbay.unit.resource;

import com.fullbay.unit.model.dto.CreateUnitFromVinRequest;
import com.fullbay.unit.model.dto.PartDto;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.model.response.ApiResponse;
import com.fullbay.unit.service.AcesLookupService;
import com.fullbay.unit.service.UnitService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/** REST resource for Unit management. */
@Path("/units")
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Units", description = "Unit management operations")
public class UnitResource {

    private final UnitService unitService;
    private final AcesLookupService acesLookupService;

    /**
     * List units or search by query parameters.
     *
     * @param customerId Optional customer ID to filter
     * @param vin Optional VIN to search
     * @return API response with units
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List or search units",
            description = "List all units or search by customer ID or VIN")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Units retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = Unit.class)))
            })
    public ApiResponse<Map<String, Object>> listUnits(
            @QueryParam("customerId")
                    @Parameter(
                            name = "customerId",
                            description = "Filter units by customer ID (optional)")
                    String customerId,
            @QueryParam("vin")
                    @Parameter(name = "vin", description = "Filter units by VIN (optional)")
                    String vin) {
        log.info("List units request - customerId: {}, vin: {}", customerId, vin);

        if (customerId != null && !customerId.isEmpty() && vin != null && !vin.isEmpty()) {
            final List<Unit> units = unitService.getUnitByCustomerIdAndVin(customerId, vin);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", units, "count", units.size()))
                    .build();
        }

        if (customerId != null && !customerId.isEmpty()) {
            final List<Unit> units = unitService.getUnitsByCustomerId(customerId);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", units, "count", units.size()))
                    .build();
        }

        if (vin != null && !vin.isEmpty()) {
            final List<Unit> units = unitService.getUnitsByVin(vin);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", units, "count", units.size()))
                    .build();
        }

        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("items", List.of(), "count", 0))
                .build();
    }

    /**
     * Get a single unit by ID.
     *
     * @param unitId The unit ID
     * @return API response with unit
     */
    @GET
    @Path("/{unitId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get unit by ID", description = "Retrieve a specific unit by its ID")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Unit retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = Unit.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Unit> getUnit(@PathParam("unitId") String unitId) {
        log.info("Get unit request - unitId: {}", unitId);
        final Unit unit = unitService.getUnitById(unitId);
        return ApiResponse.<Unit>builder().data(unit).build();
    }

    /**
     * Create a new unit from VIN via NHTSA API.
     *
     * @param request The create from VIN request (vin and customerId)
     * @return API response with created unit
     */
    @POST
    @Path("/vin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create unit from VIN",
            description =
                    "Create a new unit by decoding VIN via NHTSA API. Populates vehicle data automatically.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "201",
                        description = "Unit created successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = Unit.class))),
                @APIResponse(responseCode = "400", description = "Invalid request"),
                @APIResponse(
                        responseCode = "409",
                        description = "Unit with this VIN already exists")
            })
    public Response createUnitFromVin(@Valid CreateUnitFromVinRequest request) {
        log.info(
                "Create unit from VIN request - customerId: {}, vin: {}",
                request.getCustomerId(),
                request.getVin());
        final Unit unit = unitService.createUnitFromVin(request.getVin(), request.getCustomerId());
        final ApiResponse<Unit> response = ApiResponse.<Unit>builder().data(unit).build();
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Update an existing unit.
     *
     * @param unitId The unit ID
     * @param request The update request
     * @return API response with updated unit
     */
    @PUT
    @Path("/{unitId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Update unit",
            description = "Update an existing unit (partial updates supported)")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Unit updated successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = Unit.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Unit> updateUnit(
            @PathParam("unitId") String unitId, @Valid UpdateUnitRequest request) {
        log.info("Update unit request - unitId: {}", unitId);
        final Unit unit = unitService.updateUnit(unitId, request);
        return ApiResponse.<Unit>builder().data(unit).build();
    }

    /**
     * Delete a unit.
     *
     * @param unitId The unit ID
     * @return 204 No Content response
     */
    @DELETE
    @Path("/{unitId}")
    @Operation(summary = "Delete unit", description = "Delete a unit by ID")
    @APIResponses(
            value = {
                @APIResponse(responseCode = "204", description = "Unit deleted successfully"),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public Response deleteUnit(@PathParam("unitId") String unitId) {
        log.info("Delete unit request - unitId: {}", unitId);
        unitService.deleteUnit(unitId);
        return Response.noContent().build();
    }

    /**
     * List aftermarket parts that fit a unit, optionally filtered by category.
     *
     * @param unitId The unit ID
     * @param category Optional part category filter
     * @return API response with parts list
     */
    @GET
    @Path("/{unitId}/parts")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List parts for unit",
            description =
                    "List aftermarket parts that fit this unit based on ACES fitment data. Optionally filter by part category.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Parts retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = PartDto.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Map<String, Object>> listPartsForUnit(
            @PathParam("unitId") String unitId,
            @QueryParam("category")
                    @Parameter(
                            name = "category",
                            description = "Filter parts by category (optional)")
                    String category) {
        log.info("List parts request - unitId: {}, category: {}", unitId, category);

        final Unit unit = unitService.getUnitById(unitId);

        if (unit.baseVehicleId() == null) {
            log.info("Unit {} has no baseVehicleId - returning empty parts list", unitId);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", List.of(), "count", 0))
                    .build();
        }

        final List<PartDto> parts =
                acesLookupService.findPartsForVehicle(
                        unit.baseVehicleId(), unit.engineBaseId(), category);
        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("items", parts, "count", parts.size()))
                .build();
    }

    /**
     * List distinct part categories available for a unit.
     *
     * @param unitId The unit ID
     * @return API response with categories list
     */
    @GET
    @Path("/{unitId}/parts/categories")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List part categories for unit",
            description =
                    "List distinct part categories available for this unit based on ACES fitment data.")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Categories retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema =
                                                @Schema(
                                                        type = SchemaType.ARRAY,
                                                        implementation = String.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Map<String, Object>> listPartCategoriesForUnit(
            @PathParam("unitId") String unitId) {
        log.info("List part categories request - unitId: {}", unitId);

        final Unit unit = unitService.getUnitById(unitId);

        if (unit.baseVehicleId() == null) {
            log.info("Unit {} has no baseVehicleId - returning empty categories list", unitId);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", List.of(), "count", 0))
                    .build();
        }

        final List<String> categories =
                acesLookupService.findCategoriesForVehicle(
                        unit.baseVehicleId(), unit.engineBaseId());
        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("items", categories, "count", categories.size()))
                .build();
    }
}
