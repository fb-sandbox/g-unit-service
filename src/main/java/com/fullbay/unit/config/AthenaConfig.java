package com.fullbay.unit.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;

/** Configuration for Athena client used by VCdb lookups. */
@ApplicationScoped
@Slf4j
public class AthenaConfig {

    /**
     * Produces AthenaClient bean.
     *
     * @return AthenaClient configured for AWS region us-west-2
     */
    @Produces
    @ApplicationScoped
    public AthenaClient athenaClient() {
        log.info("Initializing AthenaClient for region: US_WEST_2");
        return AthenaClient.builder().region(Region.US_WEST_2).build();
    }
}
