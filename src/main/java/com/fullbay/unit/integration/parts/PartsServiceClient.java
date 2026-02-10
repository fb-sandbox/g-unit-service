package com.fullbay.unit.integration.parts;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Parts Service client for VCDB vehicle lookups. */
@RegisterRestClient(configKey = "parts-service")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public interface PartsServiceClient {

    @GET
    @Path("/makes")
    PartsApiResponse<java.util.List<PartsMake>> findMakesByName(@QueryParam("q") String name);

    @GET
    @Path("/makes/{makeId}/models")
    PartsApiResponse<java.util.List<PartsModel>> findModelsByName(
            @PathParam("makeId") String makeId, @QueryParam("q") String name);

    @GET
    @Path("/vehicles")
    PartsApiResponse<java.util.List<PartsVehicle>> findVehicles(
            @QueryParam("year") String year,
            @QueryParam("makeId") String makeId,
            @QueryParam("modelId") String modelId);
}
