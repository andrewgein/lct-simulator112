package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class V2__load_incident_classifier extends BaseJavaMigration {

    private static final String CLASSIFIER_FILE = "incident-classifier.json";

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, Map<String, ClassifierType>> classifier = readClassifier();

        try (
                PreparedStatement typeStatement = context.getConnection().prepareStatement("""
                        INSERT INTO types (id, service_type, type_id, type_name)
                        VALUES (?, ?, ?, ?)
                        """);
                PreparedStatement instructionStatement = context.getConnection().prepareStatement("""
                        INSERT INTO type_instructions (id, type_id, instructions, position)
                        VALUES (?, ?, ?, ?)
                        """);
                PreparedStatement fieldStatement = context.getConnection().prepareStatement("""
                        INSERT INTO additional_info
                            (id, type_id, field_code, field_name, field_type, required, position)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)
        ) {
            classifier.forEach((serviceName, types) ->
                    types.forEach((typeId, type) -> {
                        UUID databaseId = UUID.randomUUID();

                        addType(typeStatement, databaseId, serviceName, typeId, type.name());
                        addInstructions(instructionStatement, databaseId, type.instructions());
                        addFields(fieldStatement, databaseId, type.fields());
                    })
            );

            typeStatement.executeBatch();
            instructionStatement.executeBatch();
            fieldStatement.executeBatch();
        }
    }

    private Map<String, Map<String, ClassifierType>> readClassifier() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(CLASSIFIER_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Файл " + CLASSIFIER_FILE + " не найден");
            }

            return new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private void addType(
            PreparedStatement statement,
            UUID databaseId,
            String serviceName,
            String typeId,
            String typeName
    ) {
        try {
            statement.setObject(1, databaseId);
            statement.setString(2, toEnumValue(serviceName));
            statement.setString(3, typeId);
            statement.setString(4, typeName);
            statement.addBatch();
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось подготовить тип " + typeName, exception);
        }
    }

    private void addInstructions(
            PreparedStatement statement,
            UUID typeId,
            List<String> instructions
    ) {
        try {
            for (int position = 0; position < instructions.size(); position++) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, typeId);
                statement.setString(3, instructions.get(position));
                statement.setInt(4, position);
                statement.addBatch();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось подготовить инструкции", exception);
        }
    }

    private void addFields(
            PreparedStatement statement,
            UUID typeId,
            List<ClassifierField> fields
    ) {
        try {
            for (int position = 0; position < fields.size(); position++) {
                ClassifierField field = fields.get(position);

                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, typeId);
                statement.setString(3, field.id());
                statement.setString(4, field.name());
                statement.setString(5, toEnumValue(field.type()));
                statement.setBoolean(6, field.required());
                statement.setInt(7, position);
                statement.addBatch();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось подготовить дополнительные поля", exception);
        }
    }

    private String toEnumValue(String value) {
        return value.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private record ClassifierType(
            String name,
            List<ClassifierField> fields,
            List<String> instructions
    ) {
    }

    private record ClassifierField(
            String id,
            String name,
            String type,
            boolean required
    ) {
    }
}
