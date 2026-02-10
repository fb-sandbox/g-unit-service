package com.fullbay.unit.resource;

import com.fullbay.unit.model.response.ApiResponse;
import com.fullbay.unit.service.AcesLookupService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/** REST resource for global parts lookups (not unit-specific). */
@Path("/parts")
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Parts", description = "Parts lookup operations")
public class PartsResource {

    private final AcesLookupService acesLookupService;

    /**
     * List all distinct part categories across all vehicles.
     *
     * @return API response with categories list
     */
    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "List all part categories",
            description = "List all distinct part categories from the ACES parts catalog.")
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
                                                        implementation = String.class)))
            })
    public ApiResponse<Map<String, Object>> listAllCategories() {
        log.info("List all part categories request");
        final List<String> categories = acesLookupService.findAllCategories();
        return ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("items", categories, "count", categories.size()))
                .build();
    }
}
