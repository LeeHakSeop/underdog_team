package aaa.work_order_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkOrderMapper {
    @Select("SELECT * FROM work_order ORDER BY work_order_id DESC")
    List<WorkOrderDTO> list();

    @Insert("""
            INSERT INTO work_order (
                work_type,
                vehicle_id,
                driver_id,
                container_id,
                reserved_time,
                work_status,
                is_approved
            ) VALUES (
                #{workType},
                #{vehicleId},
                #{driverId},
                #{containerId},
                #{reservedTime},
                #{workStatus},
                #{isApproved}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "workOrderId", keyColumn = "work_order_id")
    int insert(WorkOrderDTO dto);

    @Select("""
    SELECT
        wo.work_order_id AS workOrderId,
        wo.work_type AS workType,
        wo.vehicle_id AS vehicleId,
        wo.container_id AS containerId,
        c.container_number AS containerNumber,
        c.sector_id AS sectorId,
        ys.sector_name AS sectorName,
        ys.guide_message AS guideMessage
    FROM work_order wo
    JOIN container c ON wo.container_id = c.container_id
    JOIN yard_sector ys ON c.sector_id = ys.sector_id
    WHERE wo.vehicle_id = #{vehicleId}
    ORDER BY wo.reserved_time DESC
    LIMIT 1
""")
    TrailerWorkInfoDTO findTrailerWorkInfoByVehicleId(Long vehicleId);
}
