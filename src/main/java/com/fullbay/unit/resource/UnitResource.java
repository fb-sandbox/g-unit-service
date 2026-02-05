package com.fullbay.unit.resource;

import com.fullbay.unit.model.dto.CreateUnitFromVinRequest;
import com.fullbay.unit.model.dto.CreateUnitRequest;
import com.fullbay.unit.model.dto.UpdateUnitRequest;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.unit.model.response.ApiResponse;
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
                                        schema = @Schema(implementation = ApiResponse.class)))
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

        if (customerId != null && !customerId.isEmpty()) {
            final List<Unit> units = unitService.getUnitsByCustomerId(customerId);
            return ApiResponse.<Map<String, Object>>builder()
                    .data(Map.of("items", units, "count", units.size()))
                    .build();
        }

        if (vin != null && !vin.isEmpty()) {
            final Unit unit = unitService.getUnitByVin(vin);
            if (unit != null) {
                return ApiResponse.<Map<String, Object>>builder()
                        .data(Map.of("item", unit))
                        .build();
            }
            return ApiResponse.<Map<String, Object>>builder().data(Map.of("item", null)).build();
        }

        // Default: empty list
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
                                        schema = @Schema(implementation = ApiResponse.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Unit> getUnit(@PathParam("unitId") String unitId) {
        log.info("Get unit request - unitId: {}", unitId);
        final Unit unit = unitService.getUnitById(unitId);
        return ApiResponse.<Unit>builder().data(unit).build();
    }

    /**
     * Get a unit by VIN.
     *
     * @param vin The VIN to search for
     * @return API response with unit
     */
    @GET
    @Path("/vin/{vin}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get unit by VIN", description = "Retrieve a unit by its VIN")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Unit retrieved successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ApiResponse.class))),
                @APIResponse(responseCode = "404", description = "Unit not found")
            })
    public ApiResponse<Unit> getUnitByVin(
            @PathParam("vin")
                    @Parameter(name = "vin", description = "Vehicle Identification Number")
                    String vin) {
        log.info("Get unit by VIN request - vin: {}", vin);
        final Unit unit = unitService.getUnitByVin(vin);
        return ApiResponse.<Unit>builder().data(unit).build();
    }

    /**
     * Create a new unit.
     *
     * @param request The create request
     * @return API response with created unit
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create unit", description = "Create a new unit")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "201",
                        description = "Unit created successfully",
                        content =
                                @Content(
                                        mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ApiResponse.class))),
                @APIResponse(responseCode = "400", description = "Invalid request"),
                @APIResponse(
                        responseCode = "409",
                        description = "Unit with this VIN already exists")
            })
    public Response createUnit(@Valid CreateUnitRequest request) {
        log.info(
                "Create unit request - customerId: {}, vin: {}",
                request.getCustomerId(),
                request.getVin());
        final Unit unit = unitService.createUnit(request);
        final ApiResponse<Unit> response = ApiResponse.<Unit>builder().data(unit).build();
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .type(MediaType.APPLICATION_JSON)
                .build();
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
                                        schema = @Schema(implementation = ApiResponse.class))),
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
                                        schema = @Schema(implementation = ApiResponse.class))),
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
}
