package aaa.work_order_p.model;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WorkOrderMapper {
    String DETAIL_SELECT = """
        SELECT
            wo.work_order_id AS workOrderId,
            wo.work_type AS workType,
            wo.vehicle_id AS vehicleId,
            wo.vehicle_id AS trailerVehicleId,
            wo.driver_id AS driverId,
            wo.container_id AS containerId,
            wo.reserved_time AS reservedTime,
            wo.work_status AS workStatus,
            wo.is_approved AS isApproved,
            ca.carrier_id AS carrierId,
            ca.carrier_name AS carrierName,
            d.driver_name AS driverName,
            trv.plate_number AS plateNumber,
            trv.plate_number AS trailerPlateNumber,
            tractor.vehicle_id AS tractorVehicleId,
            tractor.plate_number AS tractorPlateNumber,
            c.container_number AS containerNumber,
            ys.sector_name AS sectorName
        FROM work_order wo
        LEFT JOIN driver d ON wo.driver_id = d.driver_id
        LEFT JOIN carrier ca ON d.carrier_id = ca.carrier_id
        LEFT JOIN vehicle trv ON wo.vehicle_id = trv.vehicle_id
        LEFT JOIN vehicle tractor
          ON tractor.driver_id = d.driver_id
         AND UPPER(tractor.vehicle_type) IN ('TRACTOR', '트랙터')
        LEFT JOIN container c ON wo.container_id = c.container_id
        LEFT JOIN yard_sector ys ON c.sector_id = ys.sector_id
        """;

    @Select(DETAIL_SELECT + " ORDER BY wo.work_order_id DESC")
    List<WorkOrderDTO> list();

    @Select(DETAIL_SELECT + " WHERE wo.work_order_id = #{workOrderId}")
    WorkOrderDTO detail(@Param("workOrderId") Long workOrderId);

    @Select(DETAIL_SELECT + " WHERE ca.user_id = #{userId} ORDER BY wo.work_order_id DESC")
    List<WorkOrderDTO> findByCarrierUserId(@Param("userId") Long userId);

    @Insert("""
        INSERT INTO work_order (
            work_type, vehicle_id, driver_id, container_id,
            reserved_time, work_status, is_approved
        ) VALUES (
            #{workType}, #{vehicleId}, #{driverId}, #{containerId},
            #{reservedTime}, #{workStatus}, #{isApproved}
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "workOrderId", keyColumn = "work_order_id")
    int insert(WorkOrderDTO dto);

    @Select("""
        SELECT COUNT(*)
        FROM work_order
        WHERE vehicle_id = #{vehicleId}
          AND work_status NOT IN ('COMPLETED', 'CANCELLED', 'GATE_OUT')
        """)
    int countActiveByVehicleId(@Param("vehicleId") Long vehicleId);

    @Select("""
        SELECT COUNT(*)
        FROM work_order
        WHERE driver_id = #{driverId}
          AND work_status NOT IN ('COMPLETED', 'CANCELLED', 'GATE_OUT')
        """)
    int countActiveByDriverId(@Param("driverId") Long driverId);

    @Update("UPDATE work_order SET driver_id = NULL WHERE driver_id = #{driverId}")
    int clearDriverReference(@Param("driverId") Long driverId);

    @Update("UPDATE work_order SET work_status = #{workStatus} WHERE work_order_id = #{workOrderId}")
    int updateStatus(@Param("workOrderId") Long workOrderId, @Param("workStatus") String workStatus);

    @Update("""
        UPDATE work_order
        SET is_approved = true, work_status = 'APPROVED'
        WHERE work_order_id = #{workOrderId}
          AND COALESCE(is_approved, false) = false
        """)
    int approve(@Param("workOrderId") Long workOrderId);

    @Update("""
        UPDATE vehicle
        SET driver_id = NULL,
            user_id = NULL,
            vehicle_status = CASE
                WHEN UPPER(COALESCE(vehicle_status, '')) IN ('WORKING', 'IN_PROGRESS')
                     OR vehicle_status = '작업중'
                THEN '정상'
                ELSE vehicle_status
            END
        WHERE vehicle_id = #{vehicleId}
          AND UPPER(vehicle_type) IN ('TRAILER', '트레일러')
        """)
    int resetTrailer(@Param("vehicleId") Long vehicleId);
}
