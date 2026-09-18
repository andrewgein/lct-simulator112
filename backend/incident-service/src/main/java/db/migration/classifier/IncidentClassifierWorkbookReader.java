package db.migration.classifier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class IncidentClassifierWorkbookReader {

    private static final int HEADER_LEVEL_1 = 0;
    private static final int HEADER_LEVEL_2 = 1;
    private static final int HEADER_LEVEL_3 = 2;
    private static final int FIRST_DATA_ROW = 3;
    private static final int CATEGORY_CODE_COLUMN = columnIndex("A");
    private static final int FEATURE_1_CODE_COLUMN = columnIndex("B");
    private static final int FEATURE_2_CODE_COLUMN = columnIndex("C");
    private static final int FEATURE_3_CODE_COLUMN = columnIndex("D");
    private static final int ENTRY_CODE_COLUMN = columnIndex("E");
    private static final int STATISTICAL_GROUP_COLUMN = columnIndex("F");
    private static final int FEATURE_1_NAME_COLUMN = columnIndex("G");
    private static final int FEATURE_2_NAME_COLUMN = columnIndex("H");
    private static final int FEATURE_3_NAME_COLUMN = columnIndex("I");
    private static final int ADDITIONAL_FEATURES_COLUMN = columnIndex("J");
    private static final int FINAL_NAME_COLUMN = columnIndex("K");
    private static final int EKP_35_NAME_COLUMN = columnIndex("L");
    private static final int PRIMARY_SERVICE_COLUMN = columnIndex("M");
    private static final int FIRST_ROUTING_COLUMN = columnIndex("N");
    private static final int LAST_ROUTING_COLUMN = columnIndex("CU");
    private static final int EXPECTED_CATEGORY_COUNT = 24;
    private static final int EXPECTED_ENTRY_COUNT = 1283;
    private static final int EXPECTED_ROUTING_RULE_COUNT = 22484;

    private static final NavigableMap<Integer, String> SERVICE_CODES = serviceCodes();
    private static final Map<String, String> PRIMARY_SERVICE_ALIASES = Map.ofEntries(
            Map.entry("MCHS", "MCHS"),
            Map.entry("Police", "POLICE"),
            Map.entry("AMBULANCE", "AMBULANCE"),
            Map.entry("MOSGAZ", "MOSGAZ"),
            Map.entry("MOSLIFT", "MOSLIFT"),
            Map.entry("AUTOROADS", "AUTOROADS"),
            Map.entry("MOSVODOCANAL", "MOSVODOKANAL"),
            Map.entry("METRO", "METRO"),
            Map.entry("OEK", "OEK"),
            Map.entry("MOSGORTRANS", "MOSGORTRANS"),
            Map.entry("MOESK", "MOESK"),
            Map.entry("MOEK", "MOEK"),
            Map.entry("MZD", "MZD"),
            Map.entry("MGTS", "MGTS"),
            Map.entry("MOSVODOSTOK", "MOSVODOSTOK"),
            Map.entry("MOSCOLLECTOR", "MOSCOLLECTOR"),
            Map.entry("GORMOST", "GORMOST"),
            Map.entry("GKH", "GKH"),
            Map.entry("Dep.tszn", "DEP_TSZN"),
            Map.entry("MSPPN", "MSPPN"),
            Map.entry("DepEco", "DEP_ECO"),
            Map.entry("ZEMP", "ZEMP"),
            Map.entry("МСР", "MSR"),
            Map.entry("ZODD", "ZODD")
    );

    public WorkbookData read(InputStream inputStream) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            Map<String, ServiceData> services = readServices(sheet, formatter, evaluator, mergedRegions);
            List<RoutingVariantData> variants = readRoutingVariants(sheet, formatter, evaluator, mergedRegions);
            ParsedEntries parsedEntries = readEntries(sheet, formatter, evaluator, services);
            WorkbookData data = new WorkbookData(
                    parsedEntries.categories(),
                    parsedEntries.entries(),
                    List.copyOf(services.values()),
                    variants,
                    parsedEntries.rules()
            );
            validate(data);
            return data;
        }
    }

    private Map<String, ServiceData> readServices(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        Map<String, ServiceData> services = new LinkedHashMap<>();
        SERVICE_CODES.forEach((column, code) -> services.put(code, new ServiceData(
                code,
                value(sheet, HEADER_LEVEL_1, column, formatter, evaluator, mergedRegions)
        )));
        return services;
    }

    private List<RoutingVariantData> readRoutingVariants(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        List<RoutingVariantData> variants = new ArrayList<>();
        for (int column = FIRST_ROUTING_COLUMN; column <= LAST_ROUTING_COLUMN; column++) {
            String sourceColumn = columnName(column);
            List<ConditionData> conditions = conditions(sourceColumn);
            variants.add(new RoutingVariantData(
                    serviceCode(column),
                    sourceColumn,
                    value(sheet, HEADER_LEVEL_1, column, formatter, evaluator, mergedRegions),
                    value(sheet, HEADER_LEVEL_2, column, formatter, evaluator, mergedRegions),
                    value(sheet, HEADER_LEVEL_3, column, formatter, evaluator, mergedRegions),
                    conditions.isEmpty() ? 0 : 100,
                    column - FIRST_ROUTING_COLUMN,
                    conditions
            ));
        }
        return variants;
    }

    private ParsedEntries readEntries(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            Map<String, ServiceData> services
    ) {
        Map<String, CategoryData> categories = new LinkedHashMap<>();
        Map<String, Integer> categoryPositions = new LinkedHashMap<>();
        List<EntryData> entries = new ArrayList<>();
        List<RoutingRuleData> rules = new ArrayList<>();
        String pendingCategoryName = null;
        String currentCategoryCode = null;
        String currentStatisticalGroup = null;

        for (int rowIndex = FIRST_DATA_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            String entryCode = value(row, ENTRY_CODE_COLUMN, formatter, evaluator);
            String statisticalGroup = value(row, STATISTICAL_GROUP_COLUMN, formatter, evaluator);
            if (!isEntryCode(entryCode)) {
                if (!statisticalGroup.isBlank()) {
                    pendingCategoryName = statisticalGroup;
                }
                continue;
            }

            String categoryCode = value(row, CATEGORY_CODE_COLUMN, formatter, evaluator);
            if (!categoryCode.equals(currentCategoryCode)) {
                currentCategoryCode = categoryCode;
                currentStatisticalGroup = null;
            }
            if (!statisticalGroup.isBlank()) {
                currentStatisticalGroup = statisticalGroup;
            }

            if (!categories.containsKey(categoryCode)) {
                categories.put(categoryCode, new CategoryData(
                        categoryCode,
                        pendingCategoryName == null ? categoryCode : pendingCategoryName,
                        categories.size()
                ));
                categoryPositions.put(categoryCode, 0);
            }

            String primaryServiceRaw = value(row, PRIMARY_SERVICE_COLUMN, formatter, evaluator);
            List<String> primaryServiceCodes = primaryServiceCodes(primaryServiceRaw, services);
            entries.add(new EntryData(
                    entryCode,
                    categoryCode,
                    value(row, FEATURE_1_CODE_COLUMN, formatter, evaluator),
                    value(row, FEATURE_1_NAME_COLUMN, formatter, evaluator),
                    value(row, FEATURE_2_CODE_COLUMN, formatter, evaluator),
                    value(row, FEATURE_2_NAME_COLUMN, formatter, evaluator),
                    value(row, FEATURE_3_CODE_COLUMN, formatter, evaluator),
                    value(row, FEATURE_3_NAME_COLUMN, formatter, evaluator),
                    currentStatisticalGroup,
                    value(row, ADDITIONAL_FEATURES_COLUMN, formatter, evaluator),
                    value(row, FINAL_NAME_COLUMN, formatter, evaluator),
                    value(row, EKP_35_NAME_COLUMN, formatter, evaluator),
                    primaryServiceRaw,
                    categoryPositions.compute(categoryCode, (key, position) -> position == null ? 1 : position + 1) - 1,
                    primaryServiceCodes
            ));

            for (int column = FIRST_ROUTING_COLUMN; column <= LAST_ROUTING_COLUMN; column++) {
                String rawValue = value(row, column, formatter, evaluator);
                if (!rawValue.isBlank()) {
                    RuleResult result = ruleResult(rawValue);
                    rules.add(new RoutingRuleData(
                            entryCode,
                            columnName(column),
                            result.kind(),
                            result.targetTypeName(),
                            rawValue
                    ));
                }
            }
        }

        return new ParsedEntries(List.copyOf(categories.values()), entries, rules);
    }

    private List<String> primaryServiceCodes(String rawValue, Map<String, ServiceData> services) {
        if (rawValue.isBlank()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String rawService : rawValue.split(",")) {
            String name = rawService.strip();
            String code = PRIMARY_SERVICE_ALIASES.getOrDefault(name, fallbackServiceCode(name));
            services.putIfAbsent(code, new ServiceData(code, name));
            codes.add(code);
        }
        return List.copyOf(codes);
    }

    private RuleResult ruleResult(String rawValue) {
        String normalized = rawValue.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("\\s+", "")
                .replace("–", "-")
                .replace("—", "-");
        if (normalized.equals("карточка-112")) {
            return new RuleResult("SEND_CARD", null);
        }
        if (normalized.equals("нетреагирования")) {
            return new RuleResult("NO_RESPONSE", null);
        }
        return new RuleResult("SERVICE_TYPE", rawValue);
    }

    private void validate(WorkbookData data) {
        if (data.categories().size() != EXPECTED_CATEGORY_COUNT) {
            throw new IllegalStateException("Ожидалось категорий: " + EXPECTED_CATEGORY_COUNT
                    + ", найдено: " + data.categories().size());
        }
        if (data.entries().size() != EXPECTED_ENTRY_COUNT) {
            throw new IllegalStateException("Ожидалось типов: " + EXPECTED_ENTRY_COUNT
                    + ", найдено: " + data.entries().size());
        }
        if (data.entries().stream().map(EntryData::code).distinct().count() != data.entries().size()) {
            throw new IllegalStateException("Коды типов происшествий должны быть уникальными");
        }
        data.entries().stream()
                .filter(entry -> entry.finalName().isBlank())
                .findFirst()
                .ifPresent(entry -> {
                    throw new IllegalStateException("Не заполнен итоговый тип для кода " + entry.code());
                });
        int expectedVariants = LAST_ROUTING_COLUMN - FIRST_ROUTING_COLUMN + 1;
        if (data.routingVariants().size() != expectedVariants) {
            throw new IllegalStateException("Ожидалось вариантов маршрутизации: " + expectedVariants
                    + ", найдено: " + data.routingVariants().size());
        }
        if (data.routingRules().size() != EXPECTED_ROUTING_RULE_COUNT) {
            throw new IllegalStateException("Ожидалось правил маршрутизации: " + EXPECTED_ROUTING_RULE_COUNT
                    + ", найдено: " + data.routingRules().size());
        }
    }

    private static List<ConditionData> conditions(String sourceColumn) {
        return switch (sourceColumn) {
            case "N" -> List.of(condition("ACCESS_STATUS", "NOT_EQUALS", "NO_ACCESS"));
            case "O", "S" -> List.of(condition("ACCESS_STATUS", "EQUALS", "NO_ACCESS"));
            case "Q", "AD" -> List.of(condition("THREAT_TO_PEOPLE", "EQUALS", "TRUE"));
            case "R", "AE", "AM" -> List.of(condition("CASUALTY_STATUS", "EQUALS", "PRESENT"));
            case "V" -> List.of(condition("OFFENSE_STATUS", "EQUALS", "PRESENT"));
            case "W", "Y" -> List.of(condition("VICTIM_STATUS", "EQUALS", "PRESENT"));
            case "X" -> List.of(condition("VICTIM_STATUS", "EQUALS", "NONE"));
            case "Z" -> List.of(condition("VICTIM_STATUS", "EQUALS", "NOT_ON_SCENE"));
            case "AA" -> List.of(condition("GASIFICATION", "EQUALS", "FALSE"));
            case "AB" -> List.of(condition("GASIFICATION", "EQUALS", "TRUE"));
            case "AF" -> List.of(condition("MEDICAL_HELP_REQUIRED", "EQUALS", "TRUE"));
            case "AG" -> List.of(condition("EVACUATION_REQUIRED", "EQUALS", "TRUE"));
            case "AI" -> List.of(condition("LARGE_GROUP_OR_OD", "EQUALS", "TRUE"));
            case "AN", "CP" -> List.of(condition("TRAFFIC_BLOCKED", "EQUALS", "TRUE"));
            case "AQ" -> List.of(condition("LOCATION_KIND", "EQUALS", "TUNNEL"));
            case "AR" -> List.of(condition("ROAD_USER_KIND", "EQUALS", "PEDESTRIAN"));
            case "AS" -> List.of(condition("ROAD_USER_KIND", "EQUALS", "VEHICLE"));
            case "AV" -> List.of(condition("COMMUNICATION_FACILITY", "EQUALS", "TRUE"));
            case "CA" -> List.of(condition("CONSTRUCTION_SITE", "EQUALS", "TRUE"));
            case "CD" -> List.of(condition("LISTED_OBJECT", "EQUALS", "TRUE"));
            case "CR" -> List.of(condition("POLYGON_EVENT", "EQUALS", "TRUE"));
            case "CS" -> List.of(condition("LOCATION", "EQUALS", "MOSCOW"));
            default -> List.of();
        };
    }

    private static ConditionData condition(String factCode, String operator, String expectedValue) {
        return new ConditionData(factCode, operator, expectedValue);
    }

    private static String serviceCode(int column) {
        Map.Entry<Integer, String> entry = SERVICE_CODES.floorEntry(column);
        if (entry == null) {
            throw new IllegalArgumentException("Не найдена служба для колонки " + columnName(column));
        }
        return entry.getValue();
    }

    private static NavigableMap<Integer, String> serviceCodes() {
        NavigableMap<Integer, String> codes = new TreeMap<>();
        putService(codes, "N", "MCHS");
        putService(codes, "U", "POLICE");
        putService(codes, "X", "AMBULANCE");
        putService(codes, "AA", "MOSGAZ");
        putService(codes, "AC", "ZEMP");
        putService(codes, "AH", "FSB");
        putService(codes, "AJ", "MOSOBLGAZ");
        putService(codes, "AK", "AUTOROADS");
        putService(codes, "AL", "MOSGORTRANS");
        putService(codes, "AO", "GKH");
        putService(codes, "AP", "GORMOST");
        putService(codes, "AT", "MOSCOW_CANAL");
        putService(codes, "AU", "MGTS");
        putService(codes, "AW", "METRO");
        putService(codes, "AX", "MOSVODOKANAL");
        putService(codes, "AY", "MOEK");
        putService(codes, "AZ", "MOESK");
        putService(codes, "BA", "OEK");
        putService(codes, "BB", "MOSLIFT");
        putService(codes, "BC", "ZODD");
        putService(codes, "BD", "DEP_GKH");
        putService(codes, "BE", "DEP_RBIPK");
        putService(codes, "BG", "MAYOR_OFFICE");
        putService(codes, "BH", "MOSCOLLECTOR");
        putService(codes, "BI", "MZD");
        putService(codes, "BJ", "DEP_EDUCATION");
        putService(codes, "BK", "CENTRREGIONVODHOZ");
        putService(codes, "BL", "MILITARY_COMMANDANT");
        putService(codes, "BM", "OATI");
        putService(codes, "BN", "MOSVODOSTOK");
        putService(codes, "BO", "DEP_ECO");
        putService(codes, "BP", "DEP_TSZN");
        putService(codes, "BQ", "RSVO");
        putService(codes, "BR", "EVAZHD");
        putService(codes, "BS", "MSPPN");
        putService(codes, "BT", "RITUAL");
        putService(codes, "BU", "DTU");
        putService(codes, "BV", "ROSGVARDIA");
        putService(codes, "BW", "TERRITORIAL_AUTHORITIES");
        putService(codes, "BX", "TERRITORIAL_AUTHORITIES_TINAO");
        putService(codes, "BY", "REGIONAL_AUTOROADS");
        putService(codes, "BZ", "DEP_CONSTRUCTION");
        putService(codes, "CB", "VETERINARY_COMMITTEE");
        putService(codes, "CC", "MOSZHILINSPEKTSIA");
        putService(codes, "CD", "DEP_CULTURE");
        putService(codes, "CE", "CSA_GLINKA");
        putService(codes, "CF", "NTU");
        putService(codes, "CG", "FSO");
        putService(codes, "CH", "MSR");
        putService(codes, "CJ", "TOURISM_COMMITTEE");
        putService(codes, "CK", "DEP_URBAN_POLICY");
        putService(codes, "CM", "DEFENSE_MINISTRY");
        putService(codes, "CN", "DEFENSE_MINISTRY_UAV");
        putService(codes, "CO", "TRANSPORT_ORGANIZER");
        putService(codes, "CQ", "MOSECOMONITORING");
        putService(codes, "CR", "DEFENSE_MINISTRY_CHEMICAL");
        putService(codes, "CT", "CITYENERGO");
        putService(codes, "CU", "DEP_CIVIL_CONSTRUCTION");
        return codes;
    }

    private static void putService(Map<Integer, String> services, String column, String code) {
        services.put(columnIndex(column), code);
    }

    private static String fallbackServiceCode(String value) {
        return "PRIMARY_" + value.toUpperCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static boolean isEntryCode(String value) {
        return value.matches("\\d{7,8}");
    }

    private static String value(
            Sheet sheet,
            int rowIndex,
            int columnIndex,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        Row row = sheet.getRow(rowIndex);
        String directValue = value(row, columnIndex, formatter, evaluator);
        if (!directValue.isBlank()) {
            return directValue;
        }
        for (CellRangeAddress region : mergedRegions) {
            if (region.isInRange(rowIndex, columnIndex)) {
                return value(sheet.getRow(region.getFirstRow()), region.getFirstColumn(), formatter, evaluator);
            }
        }
        return "";
    }

    private static String value(Row row, int columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        try {
            return formatter.formatCellValue(cell, evaluator).strip();
        } catch (RuntimeException exception) {
            return formatter.formatCellValue(cell).strip();
        }
    }

    private static int columnIndex(String columnName) {
        int index = 0;
        for (int position = 0; position < columnName.length(); position++) {
            index = index * 26 + columnName.charAt(position) - 'A' + 1;
        }
        return index - 1;
    }

    private static String columnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        int value = columnIndex + 1;
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.append((char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.reverse().toString();
    }

    public record WorkbookData(
            List<CategoryData> categories,
            List<EntryData> entries,
            List<ServiceData> services,
            List<RoutingVariantData> routingVariants,
            List<RoutingRuleData> routingRules
    ) {
    }

    public record CategoryData(String code, String name, int position) {
    }

    public record EntryData(
            String code,
            String categoryCode,
            String feature1Code,
            String feature1Name,
            String feature2Code,
            String feature2Name,
            String feature3Code,
            String feature3Name,
            String statisticalGroup,
            String additionalFeatures,
            String finalName,
            String ekp35Name,
            String primaryServiceRaw,
            int position,
            List<String> primaryServiceCodes
    ) {
    }

    public record ServiceData(String code, String name) {
    }

    public record RoutingVariantData(
            String serviceCode,
            String sourceColumn,
            String headerLevel1,
            String headerLevel2,
            String headerLevel3,
            int priority,
            int position,
            List<ConditionData> conditions
    ) {
    }

    public record ConditionData(String factCode, String operator, String expectedValue) {
    }

    public record RoutingRuleData(
            String entryCode,
            String sourceColumn,
            String resultKind,
            String targetTypeName,
            String rawValue
    ) {
    }

    private record ParsedEntries(
            List<CategoryData> categories,
            List<EntryData> entries,
            List<RoutingRuleData> rules
    ) {
    }

    private record RuleResult(String kind, String targetTypeName) {
    }
}
