package aaa.gate_log_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GateLogMapper {
    @Select("SELECT * FROM gate_log ORDER BY gate_log_id DESC")
    List<GateLogDTO> list();

    @Insert("""
            INSERT INTO gate_log (
                vehicle_id,
                tractor_vehicle_id,
                trailer_vehicle_id,
                gate_number,
                gate_name,
                entry_time,
                exit_time,
                in_out_type,
                process_result,
                manager_check
            ) VALUES (
                #{vehicleId},
                #{tractorVehicleId},
                #{trailerVehicleId},
                #{gateNumber},
                #{gateName},
                #{entryTime},
                #{exitTime},
                #{inOutType},
                #{processResult},
                #{managerCheck}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "gateLogId", keyColumn = "gate_log_id")
    int insert(GateLogDTO dto);
}
