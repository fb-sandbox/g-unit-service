package com.fullbay.unit.integration.nhtsa;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** NHTSA VIN Decode API client. Uses Quarkus REST client to call the NHTSA VIN decode endpoint. */
@RegisterRestClient(configKey = "nhtsa")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public interface NHTSAClient {

    @GET
    @Path("/vehicles/DecodeVin/{vin}")
    NHTSAVinDecodeResponse decodeVin(
            @PathParam("vin") String vin, @QueryParam("format") String format);
}
