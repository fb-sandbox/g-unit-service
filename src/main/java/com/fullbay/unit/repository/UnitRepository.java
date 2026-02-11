package com.fullbay.unit.repository;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.model.entity.Unit;
import com.fullbay.util.JacksonConverter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repository for DynamoDB Unit operations. */
@ApplicationScoped
@Slf4j
public class UnitRepository {

    private final DynamoDbClient dynamoDbClient;
    private final JacksonConverter jacksonConverter;
    private final String tableName;

    @Inject
    public UnitRepository(
            final DynamoDbClient dynamoDbClient,
            final JacksonConverter jacksonConverter,
            @ConfigProperty(name = "dynamodb.table.name", defaultValue = "g-unit-service")
                    final String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.jacksonConverter = jacksonConverter;
        this.tableName = tableName;
    }

    /**
     * Save or update a Unit entity as JSON. Only persists association fields (unitId, customerId,
     * vin, attributes, timestamps). Vehicle data is stored separately in VIN# items.
     *
     * @param entity The entity to save
     */
    @SneakyThrows
    public void save(Unit entity) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-save")) {
            segment.putAnnotation("unitId", entity.unitId());

            final String pk = "UNT#" + entity.unitId();
            final String sk = "UNT#" + entity.unitId();

            // Build slim Unit with only association fields; vehicle fields are null
            // and filtered out by NON_NULL serialization
            final Unit slimUnit =
                    Unit.builder()
                            .unitId(entity.unitId())
                            .customerId(entity.customerId())
                            .vin(entity.vin())
                            .attributes(entity.attributes())
                            .createdAt(entity.createdAt())
                            .updatedAt(entity.updatedAt())
                            .build();

            // Serialize slim Unit to DynamoDB MAP, filtering out NULL values
            final Map<String, AttributeValue> unitMap =
                    jacksonConverter.objectToMap(slimUnit).entrySet().stream()
                            .filter(e -> e.getValue().nul() == null || !e.getValue().nul())
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            java.util.Map.Entry::getKey,
                                            java.util.Map.Entry::getValue));

            // Store PK/SK + key fields as separate attributes + entire Unit as MAP in data field
            final Map<String, AttributeValue> item = new HashMap<>();
            item.put("PK", AttributeValue.builder().s(pk).build());
            item.put("SK", AttributeValue.builder().s(sk).build());
            item.put("customerId", AttributeValue.builder().s(entity.customerId()).build());
            item.put("vin", AttributeValue.builder().s(entity.vin()).build());
            item.put(
                    "createdAt",
                    AttributeValue.builder()
                            .s(entity.createdAt() != null ? entity.createdAt().toString() : "")
                            .build());
            item.put(
                    "updatedAt",
                    AttributeValue.builder()
                            .s(entity.updatedAt() != null ? entity.updatedAt().toString() : "")
                            .build());
            item.put("data", AttributeValue.builder().m(unitMap).build());

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

            final String pk = "UNT#" + unitId;
            final String sk = "UNT#" + unitId;

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
                log.debug("Unit not found: {}", unitId);
                return Optional.empty();
            }

            // Deserialize from DynamoDB MAP data field
            final Map<String, AttributeValue> unitMap = response.item().get("data").m();
            final Unit entity = jacksonConverter.mapToObject(unitMap, Unit.class);
            log.debug("Retrieved unit: {}", unitId);
            return Optional.of(entity);
        }
    }

    /**
     * Find units by Customer ID and VIN using GSI.
     *
     * @param customerId The customer ID
     * @param vin The VIN to search for
     * @return List of matching units
     */
    @SneakyThrows
    public List<Unit> findByCustomerIdAndVin(String customerId, String vin) {
        try (Subsegment segment =
                AWSXRay.beginSubsegment("unit-repository-findByCustomerIdAndVin")) {
            segment.putAnnotation("customerId", customerId);
            segment.putAnnotation("vin", vin);

            final QueryResponse response =
                    dynamoDbClient.query(
                            QueryRequest.builder()
                                    .tableName(tableName)
                                    .indexName("GSI1-CustomerVin")
                                    .keyConditionExpression(
                                            "customerId = :customerId AND vin = :vin")
                                    .expressionAttributeValues(
                                            Map.of(
                                                    ":customerId",
                                                    AttributeValue.builder().s(customerId).build(),
                                                    ":vin",
                                                    AttributeValue.builder().s(vin).build()))
                                    .build());

            final List<Unit> units = findByPkSk(response);
            log.debug("Found {} units for customer: {} vin: {}", units.size(), customerId, vin);
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
                                    .indexName("GSI1-CustomerVin")
                                    .keyConditionExpression("customerId = :customerId")
                                    .expressionAttributeValues(
                                            Map.of(
                                                    ":customerId",
                                                    AttributeValue.builder().s(customerId).build()))
                                    .build());

            final List<Unit> units = findByPkSk(response);
            log.debug("Found {} units for customer: {}", units.size(), customerId);
            return units;
        }
    }

    /**
     * Find units by VIN using GSI2-Vin (across all customers).
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
                                    .indexName("GSI2-Vin")
                                    .keyConditionExpression("vin = :vin")
                                    .expressionAttributeValues(
                                            Map.of(":vin", AttributeValue.builder().s(vin).build()))
                                    .build());

            final List<Unit> units = findByPkSk(response);
            log.debug("Found {} units for vin: {}", units.size(), vin);
            return units;
        }
    }

    private List<Unit> findByPkSk(QueryResponse response) {
        return response.items().stream()
                .map(item -> item.get("PK"))
                .filter(attr -> attr != null)
                .map(attr -> attr.s().replace("UNT#", ""))
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Delete a Unit by ID.
     *
     * @param unitId The unit ID to delete
     */
    public void delete(String unitId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("unit-repository-delete")) {
            segment.putAnnotation("unitId", unitId);

            final String pk = "UNT#" + unitId;
            final String sk = "UNT#" + unitId;

            dynamoDbClient.deleteItem(
                    DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(
                                    Map.of(
                                            "PK",
                                            AttributeValue.builder().s(pk).build(),
                                            "SK",
                                            AttributeValue.builder().s(sk).build()))
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
