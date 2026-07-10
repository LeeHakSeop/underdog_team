package aaa.exception_log_p.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExceptionLogMapper {

    @Select("""
            SELECT
                NULL AS exceptionLogId,
                NULL AS gateLogId,
                NULL AS vehicleId,
                plate_number,
                exception_type,
                exception_message,
                occured_time AS occurredTime,
                process_status,
                manager_action,
                processed_time
            FROM exception_log
            ORDER BY occured_time DESC
            """)
    List<ExceptionLogDTO> list();

    @Insert("""
            INSERT INTO exception_log (
                plate_number,
                exception_type,
                exception_message,
                occured_time,
                process_status,
                manager_action,
                processed_time
            ) VALUES (
                #{plateNumber},
                #{exceptionType},
                #{exceptionMessage},
                #{occurredTime},
                #{processStatus},
                #{managerAction},
                #{processedTime}
            )
            """)
    int insert(ExceptionLogDTO dto);

    @Update("""
            UPDATE exception_log
            SET
                process_status = #{processStatus},
                manager_action = #{managerAction},
                processed_time = #{processedTime}
            WHERE plate_number = #{plateNumber}
              AND exception_type = #{exceptionType}
              AND occured_time = #{occurredTime}
            """)
    int updateProcess(ExceptionLogDTO dto);
}
