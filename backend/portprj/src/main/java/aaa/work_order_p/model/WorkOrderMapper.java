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
        c.container_location AS containerLocation,
        CONCAT(c.block, '-', c.bay, '-', c.row_no) AS yardLocation,
        c.block AS block,
        c.bay AS bay,
        c.row_no AS rowNo,
        c.sector_id AS sectorId,
        ys.sector_name AS sectorName,
        ys.block_name AS blockName,
        ys.guide_message AS guideMessage,
        CONCAT(
            '컨테이너 번호는 ', c.container_number,
            '이고, 해야 할 작업은 ', wo.work_type,
            ', 야드 위치는 ', COALESCE(c.container_location, ys.sector_name),
            ' / ', c.block, '-', c.bay, '-', c.row_no,
            ' 입니다.'
        ) AS workGuideMessage
    FROM work_order wo
    JOIN container c ON wo.container_id = c.container_id
    JOIN yard_sector ys ON c.sector_id = ys.sector_id
    WHERE wo.vehicle_id = #{vehicleId}
    ORDER BY wo.reserved_time DESC
    LIMIT 1
""")
    TrailerWorkInfoDTO findTrailerWorkInfoByVehicleId(Long vehicleId);
}
