package com.fullbay.unit.repository;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.util.DynamoItemHelper;
import com.fullbay.util.JacksonConverter;

import jakarta.enterprise.context.ApplicationScoped;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repository for DynamoDB Unit operations. */
@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UnitRepository {

    private final DynamoDbClient dynamoDbClient;
    private final JacksonConverter jacksonConverter;

    @ConfigProperty(name = "dynamodb.table.name", defaultValue = "g-unit-service")
    String tableName;

    /**
     * Save or update a Unit entity as JSON.
     *
     * @param entity The entity to save
     */
    @SneakyThrows
    public void save(Unit entity) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-save")) {
            segment.putAnnotation("unitId", entity.unitId());

            final Map<String, AttributeValue> item =
                    DynamoItemHelper.builder()
                            .s("unitId", entity.unitId())
                            .s("customerId", entity.customerId())
                            .s("vin", entity.vin())
                            .s(
                                    "createdAt",
                                    entity.createdAt() != null ? entity.createdAt().toString() : "")
                            .map(entity)
                            .build();

            dynamoDbClient.putItem(req -> req.tableName(tableName).item(item));
            log.debug("Saved unit: {}", entity.unitId());
        }
    }

    /**
     * Find a Unit by ID.
     *
     * @param unitId The unit ID
     * @return Optional containing the unit if found
     */
    @SneakyThrows
    public Optional<Unit> findById(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-findById")) {
            segment.putAnnotation("unitId", unitId);

            final GetItemResponse response =
                    dynamoDbClient.getItem(
                            GetItemRequest.builder()
                                    .tableName(tableName)
                                    .key(
                                            Map.of(
                                                    "unitId",
                                                    AttributeValue.builder().s(unitId).build()))
                                    .build());

            if (!response.hasItem()) {
                log.debug("Unit not found: {}", unitId);
                return Optional.empty();
            }

            final Unit entity = jacksonConverter.mapToObject(response.item(), Unit.class);
            log.debug("Retrieved unit: {}", unitId);
            return Optional.of(entity);
        }
    }

    /**
     * Find units by VIN using GSI.
     *
     * @param vin The VIN to search for
     * @return List of matching units
     */
    @SneakyThrows
    public List<Unit> findByVin(String vin) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-findByVin")) {
            segment.putAnnotation("vin", vin);

            final QueryResponse response =
                    dynamoDbClient.query(
                            QueryRequest.builder()
                                    .tableName(tableName)
                                    .indexName("vin-index")
                                    .keyConditionExpression("vin = :vin")
                                    .expressionAttributeValues(
                                            Map.of(":vin", AttributeValue.builder().s(vin).build()))
                                    .build());

            final List<Unit> units =
                    response.items().stream()
                            .map(item -> item.get("unitId"))
                            .filter(attr -> attr != null)
                            .map(AttributeValue::s)
                            .map(this::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

            log.debug("Found {} units by VIN: {}", units.size(), vin);
            return units;
        }
    }

    /**
     * Find units by Customer ID using GSI.
     *
     * @param customerId The customer ID to search for
     * @return List of matching units
     */
    @SneakyThrows
    public List<Unit> findByCustomerId(String customerId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-findByCustomerId")) {
            segment.putAnnotation("customerId", customerId);

            final QueryResponse response =
                    dynamoDbClient.query(
                            QueryRequest.builder()
                                    .tableName(tableName)
                                    .indexName("customerId-index")
                                    .keyConditionExpression("customerId = :customerId")
                                    .expressionAttributeValues(
                                            Map.of(
                                                    ":customerId",
                                                    AttributeValue.builder().s(customerId).build()))
                                    .build());

            final List<Unit> units =
                    response.items().stream()
                            .map(item -> item.get("unitId"))
                            .filter(attr -> attr != null)
                            .map(AttributeValue::s)
                            .map(this::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

            log.debug("Found {} units for customer: {}", units.size(), customerId);
            return units;
        }
    }

    @SneakyThrows
    private Unit deserializeUnit(java.util.Map<String, AttributeValue> item) {
        return jacksonConverter.mapToObject(item, Unit.class);
    }

    /**
     * Delete a Unit by ID.
     *
     * @param unitId The unit ID to delete
     */
    public void delete(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-delete")) {
            segment.putAnnotation("unitId", unitId);

            dynamoDbClient.deleteItem(
                    DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(Map.of("unitId", AttributeValue.builder().s(unitId).build()))
                            .build());

            log.debug("Deleted unit: {}", unitId);
        }
    }

    /**
     * Update a Unit entity (same as save - overwrites).
     *
     * @param entity The entity to update
     */
    public void update(Unit entity) {
        save(entity);
    }
}
