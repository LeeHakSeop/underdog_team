package aaa.predictive_maintenance_p.service;

import aaa.predictive_maintenance_p.model.PredictiveDataSummary;
import aaa.predictive_maintenance_p.model.PredictiveImportResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PredictiveMaintenanceImportService {

    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter CSV_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, EquipmentDefinition> LEGACY_EQUIPMENT_MAPPING = Map.ofEntries(
            Map.entry("ANT-001", new EquipmentDefinition("GAT-001", "게이트 자동인식 장치 01", "GATE_RECOGNITION", "GATE-01")),
            Map.entry("ANT-002", new EquipmentDefinition("GAT-002", "게이트 자동인식 장치 02", "GATE_RECOGNITION", "GATE-02")),
            Map.entry("ANT-003", new EquipmentDefinition("GAT-003", "게이트 자동인식 장치 03", "GATE_RECOGNITION", "GATE-03")),
            Map.entry("ANT-004", new EquipmentDefinition("GAT-004", "게이트 자동인식 장치 04", "GATE_RECOGNITION", "GATE-04")),
            Map.entry("ANT-005", new EquipmentDefinition("QC-001", "안벽 컨테이너 크레인 제어장치 01", "QUAY_CRANE", "QUAY-01")),
            Map.entry("ANT-006", new EquipmentDefinition("QC-002", "안벽 컨테이너 크레인 제어장치 02", "QUAY_CRANE", "QUAY-02")),
            Map.entry("ANT-007", new EquipmentDefinition("QC-003", "안벽 컨테이너 크레인 제어장치 03", "QUAY_CRANE", "QUAY-03")),
            Map.entry("ANT-008", new EquipmentDefinition("QC-004", "안벽 컨테이너 크레인 제어장치 04", "QUAY_CRANE", "QUAY-04")),
            Map.entry("ANT-009", new EquipmentDefinition("QC-005", "안벽 컨테이너 크레인 제어장치 05", "QUAY_CRANE", "QUAY-05")),
            Map.entry("ANT-010", new EquipmentDefinition("QC-006", "안벽 컨테이너 크레인 제어장치 06", "QUAY_CRANE", "QUAY-06")),
            Map.entry("ANT-011", new EquipmentDefinition("QC-007", "안벽 컨테이너 크레인 제어장치 07", "QUAY_CRANE", "QUAY-07")),
            Map.entry("ANT-012", new EquipmentDefinition("QC-008", "안벽 컨테이너 크레인 제어장치 08", "QUAY_CRANE", "QUAY-08")),
            Map.entry("ANT-013", new EquipmentDefinition("TC-001", "트랜스퍼 크레인 제어장치 01", "TRANSFER_CRANE", "YARD-TC-01")),
            Map.entry("ANT-014", new EquipmentDefinition("TC-002", "트랜스퍼 크레인 제어장치 02", "TRANSFER_CRANE", "YARD-TC-02")),
            Map.entry("ANT-015", new EquipmentDefinition("TC-003", "트랜스퍼 크레인 제어장치 03", "TRANSFER_CRANE", "YARD-TC-03")),
            Map.entry("ANT-016", new EquipmentDefinition("TC-004", "트랜스퍼 크레인 제어장치 04", "TRANSFER_CRANE", "YARD-TC-04")),
            Map.entry("ANT-017", new EquipmentDefinition("TC-005", "트랜스퍼 크레인 제어장치 05", "TRANSFER_CRANE", "YARD-TC-05")),
            Map.entry("ANT-018", new EquipmentDefinition("TC-006", "트랜스퍼 크레인 제어장치 06", "TRANSFER_CRANE", "YARD-TC-06")),
            Map.entry("ANT-019", new EquipmentDefinition("TC-007", "트랜스퍼 크레인 제어장치 07", "TRANSFER_CRANE", "YARD-TC-07")),
            Map.entry("ANT-020", new EquipmentDefinition("TC-008", "트랜스퍼 크레인 제어장치 08", "TRANSFER_CRANE", "YARD-TC-08")),
            Map.entry("ANT-021", new EquipmentDefinition("YT-001", "야드 트랙터 운행 제어장치 01", "YARD_TRACTOR", "YARD-YT-01")),
            Map.entry("ANT-022", new EquipmentDefinition("YT-002", "야드 트랙터 운행 제어장치 02", "YARD_TRACTOR", "YARD-YT-02")),
            Map.entry("ANT-023", new EquipmentDefinition("YT-003", "야드 트랙터 운행 제어장치 03", "YARD_TRACTOR", "YARD-YT-03")),
            Map.entry("ANT-024", new EquipmentDefinition("YT-004", "야드 트랙터 운행 제어장치 04", "YARD_TRACTOR", "YARD-YT-04"))
    );

    private static final String UPSERT_SENSOR_SQL = """
            INSERT INTO pm_sensor_data (
                equipment_id, collected_at,
                traffic_load, temperature_c, voltage_v, signal_strength_dbm,
                success_rate, response_time_ms, retry_count, disconnect_count,
                packet_loss_rate, error_count, days_since_maintenance,
                current_fault_probability, progression_probability, progression_model,
                anomaly_count, abnormal_sensors,
                operational_state, current_failure, precursor_entry_condition,
                needs_attention, risk_score, risk_level, source_type
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb,
                ?, ?, ?, ?, ?, ?, 'CSV'
            )
            ON CONFLICT (equipment_id, collected_at) DO UPDATE SET
                traffic_load = EXCLUDED.traffic_load,
                temperature_c = EXCLUDED.temperature_c,
                voltage_v = EXCLUDED.voltage_v,
                signal_strength_dbm = EXCLUDED.signal_strength_dbm,
                success_rate = EXCLUDED.success_rate,
                response_time_ms = EXCLUDED.response_time_ms,
                retry_count = EXCLUDED.retry_count,
                disconnect_count = EXCLUDED.disconnect_count,
                packet_loss_rate = EXCLUDED.packet_loss_rate,
                error_count = EXCLUDED.error_count,
                days_since_maintenance = EXCLUDED.days_since_maintenance,
                current_fault_probability = EXCLUDED.current_fault_probability,
                progression_probability = EXCLUDED.progression_probability,
                progression_model = EXCLUDED.progression_model,
                anomaly_count = EXCLUDED.anomaly_count,
                abnormal_sensors = EXCLUDED.abnormal_sensors,
                operational_state = EXCLUDED.operational_state,
                current_failure = EXCLUDED.current_failure,
                precursor_entry_condition = EXCLUDED.precursor_entry_condition,
                needs_attention = EXCLUDED.needs_attention,
                risk_score = EXCLUDED.risk_score,
                risk_level = EXCLUDED.risk_level,
                source_type = EXCLUDED.source_type
            """;

    private static final String UPSERT_EVENT_SQL = """
            INSERT INTO pm_event (
                equipment_id, sensor_data_id, event_type, occurred_at,
                anomaly_count, current_fault_probability, abnormal_sensors,
                event_message, source_type, notification_status
            ) VALUES (
                ?, (SELECT sensor_data_id FROM pm_sensor_data
                    WHERE equipment_id = ? AND collected_at = ?),
                ?, ?, ?, ?, ?::jsonb, ?, 'DATASET', 'DEMO_NOT_SENT'
            )
            ON CONFLICT (equipment_id, event_type, occurred_at) DO UPDATE SET
                sensor_data_id = EXCLUDED.sensor_data_id,
                anomaly_count = EXCLUDED.anomaly_count,
                current_fault_probability = EXCLUDED.current_fault_probability,
                abnormal_sensors = EXCLUDED.abnormal_sensors,
                event_message = EXCLUDED.event_message,
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PredictiveMaintenanceImportService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PredictiveImportResult importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV 파일을 선택해야 합니다.");
        }
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "predictive-data.csv");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("CSV 파일만 가져올 수 있습니다.");
        }

        ParsedCsv parsed = readCsv(file);
        Map<String, Long> equipmentIds = ensureEquipment(parsed.equipment());
        List<SensorRow> rows = parsed.rows().stream()
                .map(row -> row.withEquipmentId(requiredEquipmentId(equipmentIds, row.equipmentCode())))
                .toList();

        for (int start = 0; start < rows.size(); start += BATCH_SIZE) {
            List<SensorRow> batch = rows.subList(start, Math.min(start + BATCH_SIZE, rows.size()));
            jdbcTemplate.batchUpdate(UPSERT_SENSOR_SQL, batch, batch.size(), this::setSensorParameters);
        }

        List<EventRow> events = buildEvents(rows);
        if (!events.isEmpty()) {
            jdbcTemplate.batchUpdate(UPSERT_EVENT_SQL, events, BATCH_SIZE, this::setEventParameters);
        }

        return new PredictiveImportResult(
                filename,
                equipmentIds.size(),
                rows.size(),
                events.size(),
                rows.get(0).collectedAt(),
                rows.get(rows.size() - 1).collectedAt()
        );
    }

    public PredictiveDataSummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM pm_equipment) AS equipment_count,
                    COUNT(*) AS sensor_count,
                    (SELECT COUNT(*) FROM pm_event) AS event_count,
                    MIN(collected_at) AS first_collected_at,
                    MAX(collected_at) AS last_collected_at
                FROM pm_sensor_data
                """, (rs, rowNum) -> new PredictiveDataSummary(
                rs.getLong("equipment_count"),
                rs.getLong("sensor_count"),
                rs.getLong("event_count"),
                rs.getTimestamp("first_collected_at") == null
                        ? null : rs.getTimestamp("first_collected_at").toLocalDateTime(),
                rs.getTimestamp("last_collected_at") == null
                        ? null : rs.getTimestamp("last_collected_at").toLocalDateTime()
        ));
    }

    private ParsedCsv readCsv(MultipartFile file) throws IOException {
        List<SensorRow> rows = new ArrayList<>();
        Map<String, EquipmentSeed> equipment = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV 헤더가 없습니다.");
            }
            headerLine = headerLine.replace("\uFEFF", "");
            String[] headers = headerLine.split(",", -1);
            Map<String, Integer> indexes = new HashMap<>();
            for (int i = 0; i < headers.length; i++) indexes.put(headers[i].trim(), i);
            requireHeaders(indexes, "equipment_id", "collected_at");

            Map<String, Boolean> previousFailure = new HashMap<>();
            Map<String, String> previousState = new HashMap<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);
                try {
                    String rawEquipmentCode = requiredText(value(indexes, values, "equipment_id"), "equipment_id");
                    EquipmentDefinition equipmentDefinition = equipmentDefinition(
                            rawEquipmentCode,
                            nullableText(value(indexes, values, "location_id")),
                            nullableText(value(indexes, values, "equipment_type"))
                    );
                    String equipmentCode = equipmentDefinition.code();
                    LocalDateTime collectedAt = parseDateTime(value(indexes, values, "collected_at"));
                    equipment.putIfAbsent(equipmentCode,
                            new EquipmentSeed(equipmentCode, equipmentDefinition.name(),
                                    equipmentDefinition.locationCode(), equipmentDefinition.type()));

                    String state = normalizeState(value(indexes, values, "operational_state"));
                    boolean currentFailure = bool(value(indexes, values, "current_failure"));
                    boolean referenceFailure = bool(firstValue(indexes, values,
                            "reference_failure_event", "failure_event"));
                    boolean failureStart = referenceFailure
                            && !previousFailure.getOrDefault(equipmentCode, false);
                    boolean stateChanged = indexes.containsKey("state_changed")
                            ? bool(value(indexes, values, "state_changed"))
                            : !state.equals(previousState.get(equipmentCode));

                    rows.add(new SensorRow(
                            null, equipmentCode, collectedAt,
                            decimal(indexes, values, "traffic_load"),
                            decimal(indexes, values, "temperature_c"),
                            decimal(indexes, values, "voltage_v"),
                            decimal(indexes, values, "signal_strength_dbm"),
                            decimal(indexes, values, "success_rate"),
                            decimal(indexes, values, "response_time_ms"),
                            integer(indexes, values, "retry_count"),
                            integer(indexes, values, "disconnect_count"),
                            decimal(indexes, values, "packet_loss_rate"),
                            integer(indexes, values, "error_count"),
                            integer(indexes, values, "days_since_maintenance"),
                            decimal(indexes, values, "current_fault_probability"),
                            decimal(indexes, values, "progression_probability"),
                            nullableText(value(indexes, values, "progression_model")),
                            integerOrZero(indexes, values, "anomaly_count"),
                            abnormalSensorsJson(value(indexes, values, "abnormal_sensors")),
                            state, currentFailure,
                            bool(value(indexes, values, "precursor_entry_condition")),
                            bool(value(indexes, values, "needs_attention")),
                            decimal(indexes, values, "risk_score"),
                            nullableText(value(indexes, values, "risk_level")),
                            stateChanged, failureStart,
                            bool(value(indexes, values, "maintenance_event"))
                    ));
                    previousFailure.put(equipmentCode, referenceFailure);
                    previousState.put(equipmentCode, state);
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("CSV " + lineNumber + "행 오류: " + error.getMessage(), error);
                }
            }
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("CSV에 데이터 행이 없습니다.");
        rows.sort((left, right) -> {
            int equipmentOrder = left.equipmentCode().compareTo(right.equipmentCode());
            return equipmentOrder != 0 ? equipmentOrder : left.collectedAt().compareTo(right.collectedAt());
        });
        return new ParsedCsv(rows, equipment);
    }

    private Map<String, Long> ensureEquipment(Map<String, EquipmentSeed> equipment) {
        for (EquipmentSeed seed : equipment.values()) {
            jdbcTemplate.update("""
                    INSERT INTO pm_equipment (
                        equipment_code, equipment_name, equipment_type, location_code
                    ) VALUES (?, ?, ?, ?)
                    ON CONFLICT (equipment_code) DO UPDATE SET
                        equipment_name = EXCLUDED.equipment_name,
                        equipment_type = EXCLUDED.equipment_type,
                        location_code = COALESCE(EXCLUDED.location_code, pm_equipment.location_code),
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    seed.code(), seed.name(),
                    normalizeEquipmentType(seed.type()), seed.locationCode());
        }
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT equipment_id, equipment_code FROM pm_equipment",
                (RowCallbackHandler) rs -> result.put(
                        rs.getString("equipment_code"),
                        rs.getLong("equipment_id")
                )
        );
        return result;
    }

    private List<EventRow> buildEvents(List<SensorRow> rows) {
        List<EventRow> events = new ArrayList<>();
        for (SensorRow row : rows) {
            if (row.stateChanged() && "FAILURE_EXPECTED".equals(row.operationalState())) {
                events.add(event(row, "FAILURE_EXPECTED", "고장 예상 상태 진입"));
            }
            if ((row.stateChanged() && "FAILURE".equals(row.operationalState())) || row.failureStart()) {
                events.add(event(row, "FAILURE", "실제 고장 발생"));
            }
            if (row.maintenanceEvent()) {
                events.add(event(row, "MAINTENANCE_COMPLETED", "수리 완료"));
            }
        }
        return events.stream()
                .collect(java.util.stream.Collectors.toMap(
                        event -> event.equipmentId() + "|" + event.eventType() + "|" + event.occurredAt(),
                        event -> event,
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ))
                .values().stream().toList();
    }

    private EventRow event(SensorRow row, String type, String message) {
        return new EventRow(row.equipmentId(), row.collectedAt(), type, row.anomalyCount(),
                row.currentFaultProbability(), row.abnormalSensorsJson(), message);
    }

    private void setSensorParameters(PreparedStatement statement, SensorRow row) throws SQLException {
        int index = 1;
        statement.setLong(index++, row.equipmentId());
        statement.setTimestamp(index++, Timestamp.valueOf(row.collectedAt()));
        statement.setObject(index++, row.trafficLoad());
        statement.setObject(index++, row.temperatureC());
        statement.setObject(index++, row.voltageV());
        statement.setObject(index++, row.signalStrengthDbm());
        statement.setObject(index++, row.successRate());
        statement.setObject(index++, row.responseTimeMs());
        statement.setObject(index++, row.retryCount());
        statement.setObject(index++, row.disconnectCount());
        statement.setObject(index++, row.packetLossRate());
        statement.setObject(index++, row.errorCount());
        statement.setObject(index++, row.daysSinceMaintenance());
        statement.setObject(index++, row.currentFaultProbability());
        statement.setObject(index++, row.progressionProbability());
        statement.setString(index++, row.progressionModel());
        statement.setInt(index++, row.anomalyCount());
        statement.setString(index++, row.abnormalSensorsJson());
        statement.setString(index++, row.operationalState());
        statement.setBoolean(index++, row.currentFailure());
        statement.setBoolean(index++, row.precursorEntryCondition());
        statement.setBoolean(index++, row.needsAttention());
        statement.setObject(index++, row.riskScore());
        statement.setString(index, row.riskLevel());
    }

    private void setEventParameters(PreparedStatement statement, EventRow event) throws SQLException {
        int index = 1;
        statement.setLong(index++, event.equipmentId());
        statement.setLong(index++, event.equipmentId());
        statement.setTimestamp(index++, Timestamp.valueOf(event.occurredAt()));
        statement.setString(index++, event.eventType());
        statement.setTimestamp(index++, Timestamp.valueOf(event.occurredAt()));
        statement.setInt(index++, event.anomalyCount());
        statement.setObject(index++, event.currentFaultProbability());
        statement.setString(index++, event.abnormalSensorsJson());
        statement.setString(index, event.message());
    }

    private String abnormalSensorsJson(String value) {
        List<String> sensors = value == null || value.isBlank()
                ? List.of()
                : Arrays.stream(value.split("\\|"))
                        .map(String::trim).filter(text -> !text.isEmpty()).toList();
        try {
            return objectMapper.writeValueAsString(sensors);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("이상 센서 목록을 변환할 수 없습니다.", error);
        }
    }

    private static void requireHeaders(Map<String, Integer> indexes, String... required) {
        for (String header : required) {
            if (!indexes.containsKey(header)) throw new IllegalArgumentException("필수 열 누락: " + header);
        }
    }

    private static String value(Map<String, Integer> indexes, String[] values, String column) {
        Integer index = indexes.get(column);
        return index == null || index >= values.length ? null : values[index].trim();
    }

    private static String firstValue(Map<String, Integer> indexes, String[] values, String... columns) {
        for (String column : columns) {
            if (indexes.containsKey(column)) return value(indexes, values, column);
        }
        return null;
    }

    private static String requiredText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 값이 없습니다.");
        return value;
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Double decimal(Map<String, Integer> indexes, String[] values, String column) {
        String value = value(indexes, values, column);
        return value == null || value.isBlank() ? null : Double.valueOf(value);
    }

    private static Integer integer(Map<String, Integer> indexes, String[] values, String column) {
        String value = value(indexes, values, column);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private static int integerOrZero(Map<String, Integer> indexes, String[] values, String column) {
        Integer value = integer(indexes, values, column);
        return value == null ? 0 : value;
    }

    private static boolean bool(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(requiredText(value, "collected_at"), CSV_DATE_TIME);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("collected_at 형식은 yyyy-MM-dd HH:mm:ss 이어야 합니다.");
        }
    }

    private static String normalizeState(String value) {
        if (value == null || value.isBlank()) return "NORMAL";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEquipmentType(String value) {
        if (value == null || value.isBlank() || "hipass_antenna".equalsIgnoreCase(value)) return "PORT_EQUIPMENT";
        return value.toUpperCase(Locale.ROOT);
    }

    private static EquipmentDefinition equipmentDefinition(String rawCode, String locationCode, String type) {
        EquipmentDefinition mapped = LEGACY_EQUIPMENT_MAPPING.get(rawCode);
        if (mapped != null) return mapped;
        return new EquipmentDefinition(rawCode, rawCode, normalizeEquipmentType(type), locationCode);
    }

    private static Long requiredEquipmentId(Map<String, Long> equipmentIds, String code) {
        Long id = equipmentIds.get(code);
        if (id == null) throw new IllegalArgumentException("장비를 연결할 수 없습니다: " + code);
        return id;
    }

    private record ParsedCsv(List<SensorRow> rows, Map<String, EquipmentSeed> equipment) {}
    private record EquipmentSeed(String code, String name, String locationCode, String type) {}
    private record EquipmentDefinition(String code, String name, String type, String locationCode) {}

    private record SensorRow(
            Long equipmentId, String equipmentCode, LocalDateTime collectedAt,
            Double trafficLoad, Double temperatureC, Double voltageV, Double signalStrengthDbm,
            Double successRate, Double responseTimeMs, Integer retryCount, Integer disconnectCount,
            Double packetLossRate, Integer errorCount, Integer daysSinceMaintenance,
            Double currentFaultProbability, Double progressionProbability, String progressionModel,
            int anomalyCount, String abnormalSensorsJson,
            String operationalState, boolean currentFailure, boolean precursorEntryCondition,
            boolean needsAttention, Double riskScore, String riskLevel,
            boolean stateChanged, boolean failureStart, boolean maintenanceEvent
    ) {
        SensorRow withEquipmentId(Long id) {
            return new SensorRow(id, equipmentCode, collectedAt, trafficLoad, temperatureC, voltageV,
                    signalStrengthDbm, successRate, responseTimeMs, retryCount, disconnectCount,
                    packetLossRate, errorCount, daysSinceMaintenance, currentFaultProbability,
                    progressionProbability, progressionModel,
                    anomalyCount, abnormalSensorsJson, operationalState, currentFailure,
                    precursorEntryCondition, needsAttention, riskScore, riskLevel,
                    stateChanged, failureStart, maintenanceEvent);
        }
    }

    private record EventRow(
            Long equipmentId, LocalDateTime occurredAt, String eventType,
            int anomalyCount, Double currentFaultProbability,
            String abnormalSensorsJson, String message
    ) {}
}
