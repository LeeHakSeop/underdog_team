package aaa.exception_log_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExceptionLogMapper {

    @Select("""
            SELECT
                el.exception_log_id AS exceptionLogId,
                el.gate_log_id AS gateLogId,
                el.vehicle_id AS vehicleId,
                el.plate_number,
                el.exception_type,
                el.exception_message,
                el.occurred_time AS occurredTime,
                el.process_status,
                el.manager_action,
                el.processed_time,
                gl.gate_number,
                gl.gate_name,
                gl.entry_time,
                gl.exit_time,
                gl.in_out_type,
                gl.process_result AS gateProcessResult
            FROM exception_log el
            LEFT JOIN gate_log gl ON gl.gate_log_id = el.gate_log_id
            ORDER BY el.occurred_time DESC
            """)
    List<ExceptionLogDTO> list();

    @Insert("""
            INSERT INTO exception_log (
                gate_log_id,
                vehicle_id,
                plate_number,
                exception_type,
                exception_message,
                occurred_time,
                process_status,
                manager_action,
                processed_time
            ) VALUES (
                #{gateLogId},
                #{vehicleId},
                #{plateNumber},
                #{exceptionType},
                #{exceptionMessage},
                #{occurredTime},
                #{processStatus},
                #{managerAction},
                #{processedTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "exceptionLogId", keyColumn = "exception_log_id")
    int insert(ExceptionLogDTO dto);

    @Update("""
            UPDATE exception_log
            SET
                process_status = #{processStatus},
                manager_action = #{managerAction},
                processed_time = #{processedTime}
            WHERE exception_log_id = #{exceptionLogId}
            """)
    int updateProcess(ExceptionLogDTO dto);

    @Update("""
            UPDATE exception_log
            SET
                process_status = 'PROCESSED',
                manager_action = #{managerAction},
                processed_time = #{processedTime}
            WHERE gate_log_id = #{gateLogId}
              AND (process_status IS NULL OR process_status <> 'PROCESSED')
            """)
    int updateProcessByGateLogId(
            @Param("gateLogId") Long gateLogId,
            @Param("managerAction") String managerAction,
            @Param("processedTime") java.time.LocalDateTime processedTime
    );
}
