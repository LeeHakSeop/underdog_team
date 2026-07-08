package aaa.plate_recognition_p.model;

import aaa.gate_log_p.model.GateLogDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PlateRecognitionMapper {

    @Insert("""
            INSERT INTO gate_log (
                vehicle_id,
                gate_number,
                gate_name,
                entry_time,
                exit_time,
                in_out_type,
                process_result,
                manager_check
            ) VALUES (
                #{vehicleId},
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
    int insertGateLog(GateLogDTO dto);

    @Insert("""
            INSERT INTO plate_recognition (
                gate_log_id,
                vehicle_image,
                recognized_plate,
                is_success,
                confidence,
                manual_correction,
                error_message,
                recognition_time
            ) VALUES (
                #{gateLogId},
                #{vehicleImage},
                #{recognizedPlate},
                #{isSuccess},
                #{confidence},
                #{manualCorrection},
                #{errorMessage},
                #{recognitionTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "plateRecognitionId", keyColumn = "plate_recognition_id")
    int insertPlateRecognition(PlateRecognitionDTO dto);
}
