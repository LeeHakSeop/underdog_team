package aaa.exception_log_p.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExceptionLogMapper {

    @Select("""
            SELECT
                exception_log_id,
                gate_log_id,
                vehicle_id,
                plate_number,
                exception_type,
                exception_message,
                occurred_time,
                process_status,
                manager_action,
                processed_time
            FROM exception_log
            ORDER BY exception_log_id DESC
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
}
