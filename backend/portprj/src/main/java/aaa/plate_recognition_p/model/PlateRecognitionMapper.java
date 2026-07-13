package aaa.plate_recognition_p.model;

import aaa.carrier_p.model.CarrierDTO;
import aaa.container_p.model.ContainerDTO;
import aaa.driver_p.model.DriverDTO;
import aaa.gate_log_p.model.GateLogDTO;
import aaa.work_order_p.model.WorkOrderDTO;
import aaa.yard_sector_p.model.YardSectorDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PlateRecognitionMapper {

    @Select("""
            SELECT
                carrier_id,
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status
            FROM carrier
            WHERE carrier_id = #{carrierId}
            """)
    CarrierDTO findCarrier(Long carrierId);

    @Select("""
            SELECT
                driver_id,
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter
            FROM driver
            WHERE driver_id = #{driverId}
            """)
    DriverDTO findDriver(Long driverId);

    @Select("""
            SELECT
                work_order_id,
                work_type,
                vehicle_id,
                tractor_vehicle_id,
                trailer_vehicle_id,
                driver_id,
                container_id,
                reserved_time,
                work_status,
                is_approved
            FROM work_order
            WHERE tractor_vehicle_id = #{vehicleId}
               OR vehicle_id = #{vehicleId}
            ORDER BY work_order_id DESC
            LIMIT 1
            """)
    WorkOrderDTO findLatestWorkOrderByTractor(Long vehicleId);

    @Select("""
            SELECT
                work_order_id,
                work_type,
                vehicle_id,
                tractor_vehicle_id,
                trailer_vehicle_id,
                driver_id,
                container_id,
                reserved_time,
                work_status,
                is_approved
            FROM work_order
            WHERE trailer_vehicle_id = #{vehicleId}
               OR vehicle_id = #{vehicleId}
            ORDER BY work_order_id DESC
            LIMIT 1
            """)
    WorkOrderDTO findLatestWorkOrderByTrailer(Long vehicleId);

    @Select("""
            SELECT
                container_id,
                container_number,
                container_size,
                container_location,
                sector_id,
                block,
                bay,
                row_no,
                can_exit,
                seal_number,
                shipping_line
            FROM container
            WHERE container_id = #{containerId}
            """)
    ContainerDTO findContainer(Long containerId);

    @Select("""
            SELECT
                sector_id,
                sector_name,
                block_name,
                sector_status,
                waiting_vehicle_count,
                guide_message,
                alt_waiting_area
            FROM yard_sector
            WHERE sector_id = #{sectorId}
            """)
    YardSectorDTO findYardSector(Long sectorId);

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
    int insertGateLog(GateLogDTO dto);

    @Insert("""
            INSERT INTO plate_recognition (
                gate_log_id,
                vehicle_image,
                recognized_plate,
                plate_type,
                is_success,
                confidence,
                manual_correction,
                error_message,
                recognition_time
            ) VALUES (
                #{gateLogId},
                #{vehicleImage},
                #{recognizedPlate},
                #{plateType},
                #{isSuccess},
                #{confidence},
                #{manualCorrection},
                #{errorMessage},
                #{recognitionTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "plateRecognitionId", keyColumn = "plate_recognition_id")
    int insertPlateRecognition(PlateRecognitionDTO dto);

    @Select("""
            SELECT
                plate_recognition_id AS plateRecognitionId,
                gate_log_id AS gateLogId,
                vehicle_image AS vehicleImage,
                recognized_plate AS recognizedPlate,
                plate_type AS plateType,
                is_success AS isSuccess,
                confidence,
                manual_correction AS manualCorrection,
                error_message AS errorMessage,
                recognition_time AS recognitionTime
            FROM plate_recognition
            WHERE plate_recognition_id = #{plateRecognitionId}
            """)
    PlateRecognitionDTO detail(Long plateRecognitionId);

    @Update("""
            UPDATE plate_recognition
            SET
                manual_correction = #{manualCorrection},
                is_success = TRUE,
                error_message = NULL
            WHERE plate_recognition_id = #{plateRecognitionId}
            """)
    int updateManualCorrection(
            @Param("plateRecognitionId") Long plateRecognitionId,
            @Param("manualCorrection") String manualCorrection
    );
}
