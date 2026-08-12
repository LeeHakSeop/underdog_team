package aaa.predictive_maintenance_p.service;

import aaa.predictive_maintenance_p.model.PredictiveEquipmentResponse;
import aaa.predictive_maintenance_p.model.PredictiveEventResponse;
import aaa.predictive_maintenance_p.model.PredictiveSensorDataResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PredictiveMaintenanceQueryService {

    private static final Map<String, String> STATE_LABELS = Map.of(
            "NORMAL", "정상",
            "SUSPECT", "의심",
            "RISK", "위험",
            "FAILURE_EXPECTED", "고장 예상",
            "FAILURE", "고장",
            "POST_FAILURE_RECOVERY", "고장 후 회복"
    );

    private static final Map<String, Integer> STATE_LEVELS = Map.of(
            "NORMAL", 0,
            "SUSPECT", 1,
            "RISK", 2,
            "FAILURE_EXPECTED", 3,
            "POST_FAILURE_RECOVERY", 3,
            "FAILURE", 4
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PredictiveMaintenanceQueryService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<PredictiveEquipmentResponse> equipment() {
        return jdbcTemplate.query("""
                SELECT equipment_id, equipment_code, equipment_name, equipment_type,
                       location_code, operation_status, is_enabled
                FROM pm_equipment
                ORDER BY equipment_code
                """, (rs, rowNum) -> new PredictiveEquipmentResponse(
                rs.getLong("equipment_id"),
                rs.getString("equipment_code"),
                rs.getString("equipment_name"),
                rs.getString("equipment_type"),
                rs.getString("location_code"),
                rs.getString("operation_status"),
                rs.getBoolean("is_enabled")
        ));
    }

    public List<PredictiveSensorDataResponse> sensorData(
            String equipmentCode,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (equipmentCode == null || equipmentCode.isBlank()) {
            throw new IllegalArgumentException("equipmentCode가 필요합니다.");
        }

        StringBuilder sql = new StringBuilder("""
                WITH state_rows AS (
                    SELECT s.*,
                           LAG(s.operational_state) OVER (
                               PARTITION BY s.equipment_id ORDER BY s.collected_at
                           ) AS previous_state
                    FROM pm_sensor_data s
                    JOIN pm_equipment base_e ON base_e.equipment_id = s.equipment_id
                    WHERE base_e.equipment_code = ?
                )
                SELECT s.*, e.equipment_code,
                       EXISTS (
                           SELECT 1 FROM pm_event v
                           WHERE v.equipment_id = s.equipment_id
                             AND v.occurred_at = s.collected_at
                             AND v.event_type = 'FAILURE'
                       ) AS failure_event,
                       EXISTS (
                           SELECT 1 FROM pm_event v
                           WHERE v.equipment_id = s.equipment_id
                             AND v.occurred_at = s.collected_at
                             AND v.event_type = 'MAINTENANCE_COMPLETED'
                       ) AS maintenance_event
                FROM state_rows s
                JOIN pm_equipment e ON e.equipment_id = s.equipment_id
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(equipmentCode);
        if (from != null) {
            sql.append(" AND s.collected_at >= ?");
            parameters.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND s.collected_at <= ?");
            parameters.add(Timestamp.valueOf(to));
        }
        sql.append(" ORDER BY s.collected_at");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            String state = rs.getString("operational_state").toUpperCase(Locale.ROOT);
            String previousState = rs.getString("previous_state");
            return new PredictiveSensorDataResponse(
                    rs.getLong("sensor_data_id"),
                    rs.getString("equipment_code"),
                    rs.getTimestamp("collected_at").toLocalDateTime(),
                    nullableDouble(rs, "traffic_load"),
                    nullableDouble(rs, "temperature_c"),
                    nullableDouble(rs, "voltage_v"),
                    nullableDouble(rs, "signal_strength_dbm"),
                    nullableDouble(rs, "success_rate"),
                    nullableDouble(rs, "response_time_ms"),
                    nullableInteger(rs, "retry_count"),
                    nullableInteger(rs, "disconnect_count"),
                    nullableDouble(rs, "packet_loss_rate"),
                    nullableInteger(rs, "error_count"),
                    nullableInteger(rs, "days_since_maintenance"),
                    nullableDouble(rs, "current_fault_probability"),
                    rs.getInt("anomaly_count"),
                    jsonStringList(rs.getString("abnormal_sensors")),
                    rs.getBoolean("current_failure"),
                    state.toLowerCase(Locale.ROOT),
                    rs.getBoolean("precursor_entry_condition"),
                    previousState == null || !state.equalsIgnoreCase(previousState),
                    STATE_LABELS.getOrDefault(state, state),
                    STATE_LEVELS.getOrDefault(state, 0),
                    rs.getBoolean("needs_attention"),
                    rs.getBoolean("failure_event") ? 1 : 0,
                    rs.getBoolean("maintenance_event") ? 1 : 0
            );
        }, parameters.toArray());
    }

    public List<PredictiveEventResponse> events(String equipmentCode, String eventType) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.*, e.equipment_code
                FROM pm_event v
                JOIN pm_equipment e ON e.equipment_id = v.equipment_id
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (equipmentCode != null && !equipmentCode.isBlank()) {
            sql.append(" AND e.equipment_code = ?");
            parameters.add(equipmentCode);
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND v.event_type = ?");
            parameters.add(eventType.toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY v.occurred_at");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new PredictiveEventResponse(
                rs.getLong("event_id"),
                rs.getString("equipment_code"),
                rs.getString("event_type"),
                rs.getTimestamp("occurred_at").toLocalDateTime(),
                nullableInteger(rs, "anomaly_count"),
                nullableDouble(rs, "current_fault_probability"),
                jsonStringList(rs.getString("abnormal_sensors")),
                rs.getString("event_message"),
                rs.getString("source_type"),
                rs.getString("notification_status"),
                rs.getTimestamp("notification_sent_at") == null
                        ? null : rs.getTimestamp("notification_sent_at").toLocalDateTime()
        ), parameters.toArray());
    }

    private List<String> jsonStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException error) {
            return List.of();
        }
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
