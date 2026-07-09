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
                tractor_vehicle_id,
                trailer_vehicle_id,
                driver_id,
                container_id,
                reserved_time,
                work_status,
                is_approved
            ) VALUES (
                #{workType},
                #{vehicleId},
                #{tractorVehicleId},
                #{trailerVehicleId},
                #{driverId},
                #{containerId},
                #{reservedTime},
                #{workStatus},
                #{isApproved}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "workOrderId", keyColumn = "work_order_id")
    int insert(WorkOrderDTO dto);
}
