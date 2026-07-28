package aaa.work_order_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkStatusHistoryMapper {

    @Select("""
            SELECT
                h.history_id AS historyId,
                h.work_order_id AS workOrderId,
                h.prev_status AS prevStatus,
                h.new_status AS newStatus,
                h.changed_time AS changedTime,
                h.changed_by AS changedBy,
                h.reason,
                h.remark,
                wo.work_type AS workType,
                wo.reserved_time AS reservedTime,
                d.driver_id AS driverId,
                d.driver_name AS driverName,
                v.plate_number AS plateNumber,
                c.container_number AS containerNumber
            FROM work_status_history h
            JOIN work_order wo ON wo.work_order_id = h.work_order_id
            LEFT JOIN driver d ON d.driver_id = wo.driver_id
            LEFT JOIN vehicle v ON v.vehicle_id = COALESCE(wo.tractor_vehicle_id, wo.vehicle_id)
            LEFT JOIN container c ON c.container_id = wo.container_id
            ORDER BY h.changed_time DESC NULLS LAST, h.history_id DESC
            """)
    List<WorkStatusHistoryDTO> list();

    @Select("""
            SELECT
                h.history_id AS historyId,
                h.work_order_id AS workOrderId,
                h.prev_status AS prevStatus,
                h.new_status AS newStatus,
                h.changed_time AS changedTime,
                h.changed_by AS changedBy,
                h.reason,
                h.remark,
                wo.work_type AS workType,
                wo.reserved_time AS reservedTime,
                d.driver_id AS driverId,
                d.driver_name AS driverName,
                v.plate_number AS plateNumber,
                c.container_number AS containerNumber
            FROM work_status_history h
            JOIN work_order wo ON wo.work_order_id = h.work_order_id
            JOIN driver d ON d.driver_id = wo.driver_id
            LEFT JOIN vehicle v ON v.vehicle_id = COALESCE(wo.tractor_vehicle_id, wo.vehicle_id)
            LEFT JOIN container c ON c.container_id = wo.container_id
            WHERE d.user_id = #{userId}
            ORDER BY h.changed_time DESC NULLS LAST, h.history_id DESC
            """)
    List<WorkStatusHistoryDTO> listByDriverUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO work_status_history (
                work_order_id,
                prev_status,
                new_status,
                changed_time,
                changed_by,
                reason,
                remark
            ) VALUES (
                #{workOrderId},
                #{prevStatus},
                #{newStatus},
                #{changedTime},
                #{changedBy},
                #{reason},
                #{remark}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "historyId", keyColumn = "history_id")
    int insert(WorkStatusHistoryDTO history);
}
