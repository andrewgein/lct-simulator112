package db.migration;

import db.migration.classifier.IncidentClassifierWorkbookReader;
import db.migration.classifier.IncidentClassifierWorkbookReader.WorkbookData;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class V2__load_incident_classifier extends BaseJavaMigration {

    private static final String CLASSIFIER_FILE = "classifier/incident-classifier-v046-24.xlsx";

    @Override
    public void migrate(Context context) throws Exception {
        WorkbookData classifier = readClassifier();
        Connection connection = context.getConnection();
        Map<String, UUID> categoryIds = insertCategories(connection, classifier);
        Map<String, UUID> serviceIds = insertServices(connection, classifier);
        Map<String, UUID> entryIds = insertEntries(connection, classifier, categoryIds);
        insertPrimaryServices(connection, classifier, entryIds, serviceIds);
        Map<String, UUID> variantIds = insertRoutingVariants(connection, classifier, serviceIds);
        insertRoutingConditions(connection, classifier, variantIds);
        insertRoutingRules(connection, classifier, entryIds, variantIds);
    }

    private WorkbookData readClassifier() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(CLASSIFIER_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Файл " + CLASSIFIER_FILE + " не найден");
            }
            return new IncidentClassifierWorkbookReader().read(inputStream);
        }
    }

    private Map<String, UUID> insertCategories(Connection connection, WorkbookData classifier) throws Exception {
        Map<String, UUID> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO classifier_categories (id, code, name, position)
                VALUES (?, ?, ?, ?)
                """)) {
            for (var category : classifier.categories()) {
                UUID id = UUID.randomUUID();
                ids.put(category.code(), id);
                statement.setObject(1, id);
                statement.setString(2, category.code());
                statement.setString(3, category.name());
                statement.setInt(4, category.position());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return ids;
    }

    private Map<String, UUID> insertServices(Connection connection, WorkbookData classifier) throws Exception {
        Map<String, UUID> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO dispatch_services (id, code, name)
                VALUES (?, ?, ?)
                """)) {
            for (var service : classifier.services()) {
                UUID id = UUID.randomUUID();
                ids.put(service.code(), id);
                statement.setObject(1, id);
                statement.setString(2, service.code());
                statement.setString(3, service.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return ids;
    }

    private Map<String, UUID> insertEntries(
            Connection connection,
            WorkbookData classifier,
            Map<String, UUID> categoryIds
    ) throws Exception {
        Map<String, UUID> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO classifier_entries (
                    id, category_id, code,
                    feature_1_code, feature_1_name,
                    feature_2_code, feature_2_name,
                    feature_3_code, feature_3_name,
                    statistical_group, additional_features,
                    final_name, ekp_35_name, primary_service_raw, position
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var entry : classifier.entries()) {
                UUID id = UUID.randomUUID();
                ids.put(entry.code(), id);
                statement.setObject(1, id);
                statement.setObject(2, requireId(categoryIds, entry.categoryCode(), "категория"));
                statement.setString(3, entry.code());
                statement.setString(4, emptyToNull(entry.feature1Code()));
                statement.setString(5, emptyToNull(entry.feature1Name()));
                statement.setString(6, emptyToNull(entry.feature2Code()));
                statement.setString(7, emptyToNull(entry.feature2Name()));
                statement.setString(8, emptyToNull(entry.feature3Code()));
                statement.setString(9, emptyToNull(entry.feature3Name()));
                statement.setString(10, emptyToNull(entry.statisticalGroup()));
                statement.setString(11, emptyToNull(entry.additionalFeatures()));
                statement.setString(12, entry.finalName());
                statement.setString(13, emptyToNull(entry.ekp35Name()));
                statement.setString(14, emptyToNull(entry.primaryServiceRaw()));
                statement.setInt(15, entry.position());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return ids;
    }

    private void insertPrimaryServices(
            Connection connection,
            WorkbookData classifier,
            Map<String, UUID> entryIds,
            Map<String, UUID> serviceIds
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO classifier_entry_primary_services (classifier_entry_id, dispatch_service_id)
                VALUES (?, ?)
                """)) {
            for (var entry : classifier.entries()) {
                for (String serviceCode : entry.primaryServiceCodes()) {
                    statement.setObject(1, requireId(entryIds, entry.code(), "тип происшествия"));
                    statement.setObject(2, requireId(serviceIds, serviceCode, "служба"));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private Map<String, UUID> insertRoutingVariants(
            Connection connection,
            WorkbookData classifier,
            Map<String, UUID> serviceIds
    ) throws Exception {
        Map<String, UUID> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO routing_variants (
                    id, dispatch_service_id, routing_target, source_column,
                    header_level_1, header_level_2, header_level_3,
                    priority, position
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (var variant : classifier.routingVariants()) {
                UUID id = UUID.randomUUID();
                ids.put(variant.sourceColumn(), id);
                statement.setObject(1, id);
                statement.setObject(2, requireId(serviceIds, variant.serviceCode(), "служба"));
                statement.setString(3, variant.routingTarget());
                statement.setString(4, variant.sourceColumn());
                statement.setString(5, emptyToNull(variant.headerLevel1()));
                statement.setString(6, emptyToNull(variant.headerLevel2()));
                statement.setString(7, emptyToNull(variant.headerLevel3()));
                statement.setInt(8, variant.priority());
                statement.setInt(9, variant.position());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        return ids;
    }

    private void insertRoutingConditions(
            Connection connection,
            WorkbookData classifier,
            Map<String, UUID> variantIds
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO routing_variant_conditions (
                    id, routing_variant_id, fact_code, operator, expected_value, position
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (var variant : classifier.routingVariants()) {
                for (int position = 0; position < variant.conditions().size(); position++) {
                    var condition = variant.conditions().get(position);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, requireId(variantIds, variant.sourceColumn(), "вариант маршрутизации"));
                    statement.setString(3, condition.factCode());
                    statement.setString(4, condition.operator());
                    statement.setString(5, emptyToNull(condition.expectedValue()));
                    statement.setInt(6, position);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void insertRoutingRules(
            Connection connection,
            WorkbookData classifier,
            Map<String, UUID> entryIds,
            Map<String, UUID> variantIds
    ) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO routing_rules (
                    id, classifier_entry_id, routing_variant_id,
                    result_kind, target_type_name, raw_value
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (var rule : classifier.routingRules()) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, requireId(entryIds, rule.entryCode(), "тип происшествия"));
                statement.setObject(3, requireId(variantIds, rule.sourceColumn(), "вариант маршрутизации"));
                statement.setString(4, rule.resultKind());
                statement.setString(5, emptyToNull(rule.targetTypeName()));
                statement.setString(6, rule.rawValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private UUID requireId(Map<String, UUID> ids, String code, String resource) {
        UUID id = ids.get(code);
        if (id == null) {
            throw new IllegalStateException("Не найдена " + resource + " с кодом " + code);
        }
        return id;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
