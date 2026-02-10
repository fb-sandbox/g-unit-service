package com.fullbay.unit.repository;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.model.entity.Vehicle;
import com.fullbay.util.JacksonConverter;

import jakarta.enterprise.context.ApplicationScoped;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Repository for DynamoDB Vehicle operations. Stores vehicle data once per VIN. */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class VehicleRepository {

    private final DynamoDbClient dynamoDbClient;
    private final JacksonConverter jacksonConverter;

    @ConfigProperty(name = "dynamodb.table.name", defaultValue = "g-unit-service")
    String tableName;

    private static final int BATCH_GET_CHUNK_SIZE = 100;

    /**
     * Save a Vehicle entity as JSON. PK/SK = "VIN#&lt;vin&gt;". No top-level customerId/vin
     * attributes to avoid GSI indexing.
     *
     * @param entity The vehicle to save
     */
    @SneakyThrows
    public void save(Vehicle entity) {
        try (Subsegment segment = AWSXRay.beginSubsegment("vehicle-repository-save")) {
            segment.putAnnotation("vin", entity.vin());

            final String pk = "VIN#" + entity.vin();
            final String sk = "VIN#" + entity.vin();

            final Map<String, AttributeValue> vehicleMap =
                    jacksonConverter.objectToMap(entity).entrySet().stream()
                            .filter(e -> e.getValue().nul() == null || !e.getValue().nul())
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            java.util.Map.Entry::getKey,
                                            java.util.Map.Entry::getValue));

            final Map<String, AttributeValue> item = new HashMap<>();
            item.put("PK", AttributeValue.builder().s(pk).build());
            item.put("SK", AttributeValue.builder().s(sk).build());
            item.put("data", AttributeValue.builder().m(vehicleMap).build());

            dynamoDbClient.putItem(req -> req.tableName(tableName).item(item));
            log.debug("Saved vehicle: {}", entity.vin());
        }
    }

    /**
     * Find a Vehicle by VIN.
     *
     * @param vin The VIN
     * @return Optional containing the vehicle if found
     */
    @SneakyThrows
    public Optional<Vehicle> findByVin(String vin) {
        try (Subsegment segment = AWSXRay.beginSubsegment("vehicle-repository-findByVin")) {
            segment.putAnnotation("vin", vin);

            final String pk = "VIN#" + vin;
            final String sk = "VIN#" + vin;

            final GetItemResponse response =
                    dynamoDbClient.getItem(
                            GetItemRequest.builder()
                                    .tableName(tableName)
                                    .key(
                                            Map.of(
                                                    "PK",
                                                    AttributeValue.builder().s(pk).build(),
                                                    "SK",
                                                    AttributeValue.builder().s(sk).build()))
                                    .build());

            if (!response.hasItem()) {
                log.debug("Vehicle not found: {}", vin);
                return Optional.empty();
            }

            final Map<String, AttributeValue> vehicleMap = response.item().get("data").m();
            final Vehicle entity = jacksonConverter.mapToObject(vehicleMap, Vehicle.class);
            log.debug("Retrieved vehicle: {}", vin);
            return Optional.of(entity);
        }
    }

    /**
     * Find multiple Vehicles by VINs using BatchGetItem. Chunked at 100 items per request.
     *
     * @param vins The set of VINs to look up
     * @return Map of VIN to Vehicle for found items
     */
    @SneakyThrows
    public Map<String, Vehicle> findByVins(Set<String> vins) {
        try (Subsegment segment = AWSXRay.beginSubsegment("vehicle-repository-findByVins")) {
            segment.putAnnotation("vinCount", vins.size());

            if (vins.isEmpty()) {
                return Map.of();
            }

            final Map<String, Vehicle> result = new HashMap<>();
            final List<String> vinList = new ArrayList<>(vins);

            for (int i = 0; i < vinList.size(); i += BATCH_GET_CHUNK_SIZE) {
                final List<String> chunk =
                        vinList.subList(i, Math.min(i + BATCH_GET_CHUNK_SIZE, vinList.size()));

                final List<Map<String, AttributeValue>> keys = new ArrayList<>();
                for (final String vin : chunk) {
                    final String pk = "VIN#" + vin;
                    keys.add(
                            Map.of(
                                    "PK",
                                    AttributeValue.builder().s(pk).build(),
                                    "SK",
                                    AttributeValue.builder().s(pk).build()));
                }

                final BatchGetItemResponse response =
                        dynamoDbClient.batchGetItem(
                                BatchGetItemRequest.builder()
                                        .requestItems(
                                                Map.of(
                                                        tableName,
                                                        KeysAndAttributes.builder()
                                                                .keys(keys)
                                                                .build()))
                                        .build());

                final List<Map<String, AttributeValue>> items = response.responses().get(tableName);
                if (items != null) {
                    for (final Map<String, AttributeValue> item : items) {
                        final Map<String, AttributeValue> vehicleMap = item.get("data").m();
                        final Vehicle vehicle =
                                jacksonConverter.mapToObject(vehicleMap, Vehicle.class);
                        result.put(vehicle.vin(), vehicle);
                    }
                }
            }

            log.debug("Found {} vehicles for {} VINs", result.size(), vins.size());
            return result;
        }
    }
}
