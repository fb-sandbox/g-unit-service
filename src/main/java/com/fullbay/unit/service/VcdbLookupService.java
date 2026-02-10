package com.fullbay.unit.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.fullbay.unit.util.AthenaQueryHelper;

import jakarta.enterprise.context.ApplicationScoped;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Row;

import java.util.List;
import java.util.Optional;

/** Service for looking up VCdb BaseVehicleID via Athena queries against the g_acespies catalog. */
@ApplicationScoped
@Slf4j
public class VcdbLookupService {

    private final AthenaClient athenaClient;
    private final String workgroup;
    private final String database;
    private final String outputLocation;

    public VcdbLookupService(
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
     * Look up the VCdb BaseVehicleID for a given year, make, and model.
     *
     * @param year The vehicle model year
     * @param makeName The vehicle make name (e.g. "Ford")
     * @param modelName The vehicle model name (e.g. "F-150")
     * @return Optional containing the BaseVehicleID, or empty if not found or on error
     */
    public Optional<Integer> lookupBaseVehicleId(Integer year, String makeName, String modelName) {
        try (Subsegment segment = AWSXRay.beginSubsegment("vcdb-lookup-lookupBaseVehicleId")) {
            segment.putAnnotation("year", year != null ? year : 0);
            segment.putAnnotation("make", makeName != null ? makeName : "null");
            segment.putAnnotation("model", modelName != null ? modelName : "null");

            if (year == null || makeName == null || modelName == null) {
                log.debug(
                        "Skipping VCdb lookup - missing required fields: year={}, make={}, model={}",
                        year,
                        makeName,
                        modelName);
                return Optional.empty();
            }

            final String sql =
                    "SELECT bv.base_vehicle_id "
                            + "FROM g_acespies.vcdb_base_vehicle bv "
                            + "JOIN g_acespies.vcdb_make mk ON bv.make_id = mk.make_id "
                            + "JOIN g_acespies.vcdb_model md ON bv.model_id = md.model_id "
                            + "WHERE bv.year_id = "
                            + year
                            + " AND UPPER(mk.make_name) = UPPER('"
                            + AthenaQueryHelper.escapeSql(makeName)
                            + "') "
                            + "AND UPPER(md.model_name) = UPPER('"
                            + AthenaQueryHelper.escapeSql(modelName)
                            + "') "
                            + "LIMIT 1";

            log.debug("Executing VCdb lookup query: {}", sql);

            final String queryExecutionId =
                    AthenaQueryHelper.startQuery(
                            athenaClient, sql, workgroup, database, outputLocation);
            AthenaQueryHelper.waitForQueryCompletion(athenaClient, queryExecutionId);
            final Optional<Integer> result = getQueryResult(queryExecutionId);

            if (result.isPresent()) {
                log.info(
                        "VCdb lookup found baseVehicleId={} for year={} make={} model={}",
                        result.get(),
                        year,
                        makeName,
                        modelName);
                segment.putAnnotation("baseVehicleId", result.get());
            } else {
                log.info(
                        "VCdb lookup found no match for year={} make={} model={}",
                        year,
                        makeName,
                        modelName);
            }

            return result;
        } catch (Exception e) {
            log.warn(
                    "VCdb lookup failed for year={} make={} model={}: {}",
                    year,
                    makeName,
                    modelName,
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Look up the VCdb EngineBaseID for a given displacement, cylinder count, and engine
     * configuration.
     *
     * @param displacementLiters The engine displacement in liters (e.g. 6.7)
     * @param cylinders The number of engine cylinders (e.g. 6)
     * @param engineConfiguration The NHTSA engine configuration (e.g. "V-Shaped", "In-Line")
     * @return Optional containing the EngineBaseID, or empty if not found or on error
     */
    public Optional<Integer> lookupEngineBaseId(
            Double displacementLiters, Integer cylinders, String engineConfiguration) {
        try (Subsegment segment = AWSXRay.beginSubsegment("vcdb-lookup-lookupEngineBaseId")) {
            segment.putAnnotation(
                    "displacementLiters", displacementLiters != null ? displacementLiters : 0.0);
            segment.putAnnotation("cylinders", cylinders != null ? cylinders : 0);
            segment.putAnnotation(
                    "engineConfiguration",
                    engineConfiguration != null ? engineConfiguration : "null");

            if (displacementLiters == null || cylinders == null || engineConfiguration == null) {
                log.debug(
                        "Skipping VCdb engine base lookup - missing required fields:"
                                + " displacementLiters={}, cylinders={}, engineConfiguration={}",
                        displacementLiters,
                        cylinders,
                        engineConfiguration);
                return Optional.empty();
            }

            final String blockType = mapEngineConfigToBlockType(engineConfiguration);
            if (blockType == null) {
                log.debug(
                        "Skipping VCdb engine base lookup - unknown engine configuration: {}",
                        engineConfiguration);
                return Optional.empty();
            }

            // VCdb liter column is a string without trailing zeros (e.g. "6.7", "5")
            final String literStr = formatDisplacement(displacementLiters);
            final String cylindersStr = String.valueOf(cylinders);

            final String sql =
                    "SELECT engine_base_id "
                            + "FROM g_acespies.vcdb_engine_base "
                            + "WHERE liter = '"
                            + AthenaQueryHelper.escapeSql(literStr)
                            + "' AND cylinders = '"
                            + AthenaQueryHelper.escapeSql(cylindersStr)
                            + "' AND block_type = '"
                            + AthenaQueryHelper.escapeSql(blockType)
                            + "' "
                            + "LIMIT 1";

            log.debug("Executing VCdb engine base lookup query: {}", sql);

            final String queryExecutionId =
                    AthenaQueryHelper.startQuery(
                            athenaClient, sql, workgroup, database, outputLocation);
            AthenaQueryHelper.waitForQueryCompletion(athenaClient, queryExecutionId);
            final Optional<Integer> result = getQueryResult(queryExecutionId);

            if (result.isPresent()) {
                log.info(
                        "VCdb engine base lookup found engineBaseId={} for liter={} cylinders={}"
                                + " blockType={}",
                        result.get(),
                        literStr,
                        cylindersStr,
                        blockType);
                segment.putAnnotation("engineBaseId", result.get());
            } else {
                log.info(
                        "VCdb engine base lookup found no match for liter={} cylinders={}"
                                + " blockType={}",
                        literStr,
                        cylindersStr,
                        blockType);
            }

            return result;
        } catch (Exception e) {
            log.warn(
                    "VCdb engine base lookup failed for displacementLiters={} cylinders={}"
                            + " engineConfiguration={}: {}",
                    displacementLiters,
                    cylinders,
                    engineConfiguration,
                    e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Map NHTSA engine configuration values to VCdb block_type codes.
     *
     * @param engineConfiguration The NHTSA engine configuration string
     * @return The VCdb block_type code, or null if unknown
     */
    private static String mapEngineConfigToBlockType(String engineConfiguration) {
        switch (engineConfiguration) {
            case "V-Shaped":
                return "V";
            case "In-Line":
                return "I";
            case "Flat":
                return "H";
            case "Rotary":
                return "R";
            case "W-Shaped":
                return "W";
            default:
                return null;
        }
    }

    /**
     * Format displacement liters without trailing zeros for VCdb lookup. E.g. 6.7 → "6.7", 5.0 →
     * "5", 2.0 → "2".
     */
    private static String formatDisplacement(Double liters) {
        if (liters == liters.longValue()) {
            return String.valueOf(liters.longValue());
        }
        return String.valueOf(liters);
    }

    private Optional<Integer> getQueryResult(String queryExecutionId) {
        final List<Row> rows = AthenaQueryHelper.getResultRows(athenaClient, queryExecutionId);

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        final String value = rows.get(0).data().get(0).varCharValue();
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Integer.parseInt(value));
    }
}
