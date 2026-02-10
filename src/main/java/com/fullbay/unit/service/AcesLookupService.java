package com.fullbay.unit.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.model.dto.PartDto;
import com.fullbay.unit.util.AthenaQueryHelper;

import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.Row;

import java.util.ArrayList;
import java.util.List;

/** Service for querying ACES fitment data via Athena to find aftermarket parts for a vehicle. */
@ApplicationScoped
@Slf4j
public class AcesLookupService {

    private final AthenaClient athenaClient;
    private final String workgroup;
    private final String database;
    private final String outputLocation;

    public AcesLookupService(
            AthenaClient athenaClient,
            @ConfigProperty(name = "athena.workgroup") String workgroup,
            @ConfigProperty(name = "athena.database") String database,
            @ConfigProperty(name = "athena.output-location") String outputLocation) {
        this.athenaClient = athenaClient;
        this.workgroup = workgroup;
        this.database = database;
        this.outputLocation = outputLocation;
    }

    /**
     * Find aftermarket parts that fit a given base vehicle, optionally filtered by category.
     *
     * @param baseVehicleId The VCdb base vehicle ID
     * @param category Optional top-level category filter (case-insensitive), e.g. "Brakes"
     * @return List of matching parts, or empty list on error or null baseVehicleId
     */
    public List<PartDto> findPartsForVehicle(
            Integer baseVehicleId, Integer engineBaseId, String category) {
        try (Subsegment segment = AWSXRay.beginSubsegment("aces-lookup-findPartsForVehicle")) {
            segment.putAnnotation("baseVehicleId", baseVehicleId != null ? baseVehicleId : 0);
            segment.putAnnotation("engineBaseId", engineBaseId != null ? engineBaseId : 0);
            segment.putAnnotation("category", category != null ? category : "all");

            if (baseVehicleId == null) {
                log.debug("Skipping ACES parts lookup - baseVehicleId is null");
                return List.of();
            }

            final StringBuilder sql = new StringBuilder();
            sql.append(
                    "SELECT f.part_number, b.brand_name, c.category_name, p.part_terminology_name, ");
            sql.append("pos.position, f.qty, f.note ");
            sql.append("FROM g_acespies.aces_fitment f ");
            sql.append("LEFT JOIN g_acespies.brand b ON f.brand_aaia_id = b.brand_id ");
            sql.append("JOIN g_acespies.pcadb_parts p ON f.part_type_id = p.part_terminology_id ");
            sql.append(
                    "LEFT JOIN g_acespies.pcadb_category_parts cp ON p.part_terminology_id = cp.part_terminology_id ");
            sql.append(
                    "LEFT JOIN g_acespies.pcadb_categories c ON cp.category_id = c.category_id ");
            sql.append(
                    "LEFT JOIN g_acespies.pcadb_positions pos ON f.position_id = pos.position_id ");
            sql.append("WHERE f.base_vehicle_id = ").append(baseVehicleId);

            if (engineBaseId != null) {
                sql.append(" AND f.engine_base_id = ").append(engineBaseId);
            }

            if (category != null && !category.isEmpty()) {
                sql.append(" AND UPPER(c.category_name) = UPPER('")
                        .append(AthenaQueryHelper.escapeSql(category))
                        .append("')");
            }

            sql.append(" ORDER BY c.category_name, p.part_terminology_name, b.brand_name");

            log.debug("Executing ACES parts query: {}", sql);

            final String queryExecutionId =
                    AthenaQueryHelper.startQuery(
                            athenaClient, sql.toString(), workgroup, database, outputLocation);
            AthenaQueryHelper.waitForQueryCompletion(athenaClient, queryExecutionId);
            final List<Row> rows = AthenaQueryHelper.getResultRows(athenaClient, queryExecutionId);

            final List<PartDto> parts = new ArrayList<>();
            for (final Row row : rows) {
                final List<Datum> data = row.data();
                parts.add(
                        PartDto.builder()
                                .partNumber(getDatumValue(data, 0))
                                .brandName(getDatumValue(data, 1))
                                .category(getDatumValue(data, 2))
                                .partType(getDatumValue(data, 3))
                                .position(getDatumValue(data, 4))
                                .quantity(getDatumValueAsInt(data, 5))
                                .note(getDatumValue(data, 6))
                                .build());
            }

            log.info(
                    "ACES parts lookup found {} parts for baseVehicleId={} category={}",
                    parts.size(),
                    baseVehicleId,
                    category);
            segment.putAnnotation("resultCount", parts.size());
            return parts;
        } catch (Exception e) {
            log.warn(
                    "ACES parts lookup failed for baseVehicleId={} category={}: {}",
                    baseVehicleId,
                    category,
                    e.getMessage());
            return List.of();
        }
    }

    /**
     * Find distinct top-level part categories available for a given base vehicle.
     *
     * @param baseVehicleId The VCdb base vehicle ID
     * @return List of category names, or empty list on error or null baseVehicleId
     */
    public List<String> findCategoriesForVehicle(Integer baseVehicleId, Integer engineBaseId) {
        try (Subsegment segment = AWSXRay.beginSubsegment("aces-lookup-findCategoriesForVehicle")) {
            segment.putAnnotation("baseVehicleId", baseVehicleId != null ? baseVehicleId : 0);
            segment.putAnnotation("engineBaseId", engineBaseId != null ? engineBaseId : 0);

            if (baseVehicleId == null) {
                log.debug("Skipping ACES categories lookup - baseVehicleId is null");
                return List.of();
            }

            final StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(
                    "SELECT DISTINCT c.category_name "
                            + "FROM g_acespies.aces_fitment f "
                            + "JOIN g_acespies.pcadb_parts p ON f.part_type_id = p.part_terminology_id "
                            + "JOIN g_acespies.pcadb_category_parts cp ON p.part_terminology_id = cp.part_terminology_id "
                            + "JOIN g_acespies.pcadb_categories c ON cp.category_id = c.category_id "
                            + "WHERE f.base_vehicle_id = "
                            + baseVehicleId);

            if (engineBaseId != null) {
                sqlBuilder.append(" AND f.engine_base_id = ").append(engineBaseId);
            }

            sqlBuilder.append(" ORDER BY c.category_name");
            final String sql = sqlBuilder.toString();

            log.debug("Executing ACES categories query: {}", sql);

            final String queryExecutionId =
                    AthenaQueryHelper.startQuery(
                            athenaClient, sql, workgroup, database, outputLocation);
            AthenaQueryHelper.waitForQueryCompletion(athenaClient, queryExecutionId);
            final List<Row> rows = AthenaQueryHelper.getResultRows(athenaClient, queryExecutionId);

            final List<String> categories = new ArrayList<>();
            for (final Row row : rows) {
                final String value = getDatumValue(row.data(), 0);
                if (value != null) {
                    categories.add(value);
                }
            }

            log.info(
                    "ACES categories lookup found {} categories for baseVehicleId={}",
                    categories.size(),
                    baseVehicleId);
            segment.putAnnotation("resultCount", categories.size());
            return categories;
        } catch (Exception e) {
            log.warn(
                    "ACES categories lookup failed for baseVehicleId={}: {}",
                    baseVehicleId,
                    e.getMessage());
            return List.of();
        }
    }

    /**
     * Find all distinct top-level part categories across all vehicles with fitment data.
     *
     * @return List of all category names, or empty list on error
     */
    public List<String> findAllCategories() {
        try (Subsegment segment = AWSXRay.beginSubsegment("aces-lookup-findAllCategories")) {
            final String sql =
                    "SELECT DISTINCT c.category_name "
                            + "FROM g_acespies.aces_fitment f "
                            + "JOIN g_acespies.pcadb_parts p ON f.part_type_id = p.part_terminology_id "
                            + "JOIN g_acespies.pcadb_category_parts cp ON p.part_terminology_id = cp.part_terminology_id "
                            + "JOIN g_acespies.pcadb_categories c ON cp.category_id = c.category_id "
                            + "ORDER BY c.category_name";

            log.debug("Executing ACES all-categories query: {}", sql);

            final String queryExecutionId =
                    AthenaQueryHelper.startQuery(
                            athenaClient, sql, workgroup, database, outputLocation);
            AthenaQueryHelper.waitForQueryCompletion(athenaClient, queryExecutionId);
            final List<Row> rows = AthenaQueryHelper.getResultRows(athenaClient, queryExecutionId);

            final List<String> categories = new ArrayList<>();
            for (final Row row : rows) {
                final String value = getDatumValue(row.data(), 0);
                if (value != null) {
                    categories.add(value);
                }
            }

            log.info("ACES all-categories lookup found {} categories", categories.size());
            segment.putAnnotation("resultCount", categories.size());
            return categories;
        } catch (Exception e) {
            log.warn("ACES all-categories lookup failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String getDatumValue(List<Datum> data, int index) {
        if (index >= data.size()) {
            return null;
        }
        final String value = data.get(index).varCharValue();
        return (value == null || value.isEmpty()) ? null : value;
    }

    private static Integer getDatumValueAsInt(List<Datum> data, int index) {
        final String value = getDatumValue(data, index);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
