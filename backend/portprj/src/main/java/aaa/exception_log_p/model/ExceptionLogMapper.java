package aaa.exception_log_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExceptionLogMapper {

    @Select("""
            SELECT
                exception_log_id AS exceptionLogId,
                gate_log_id AS gateLogId,
                vehicle_id AS vehicleId,
                plate_number,
                exception_type,
                exception_message,
                occurred_time AS occurredTime,
                process_status,
                manager_action,
                processed_time
            FROM exception_log
            ORDER BY occurred_time DESC
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
