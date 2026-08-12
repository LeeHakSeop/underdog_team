package aaa.dashboard_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
            WITH sector_metrics AS (
                SELECT
                    ys.sector_id,
                    40 AS capacity,
                    COALESCE(ys.waiting_vehicle_count, 0) AS waiting_vehicle_count,
                    COUNT(DISTINCT c.container_id) AS container_count,
                    COUNT(DISTINCT wo.work_order_id) AS work_order_count,
                    CASE
                        WHEN (
                            COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                        ) >= 0.8
                          OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                          OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN 'DANGER'
                        WHEN (
                            COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                        ) >= 0.5
                          OR COALESCE(ys.waiting_vehicle_count, 0) >= 3
                          OR COUNT(DISTINCT wo.work_order_id) >= 1 THEN 'WARNING'
                        ELSE 'NORMAL'
                    END AS status_level
                FROM yard_sector ys
                LEFT JOIN container c ON c.sector_id = ys.sector_id
                LEFT JOIN work_order wo
                    ON wo.container_id = c.container_id
                   AND wo.work_status IN ('DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS')
                GROUP BY ys.sector_id, ys.waiting_vehicle_count
            )
            SELECT
                (SELECT COUNT(*) FROM vehicle) AS totalVehicles,
                (SELECT COUNT(*) FROM users WHERE status = 'PENDING') AS pendingUsers,
                (SELECT COUNT(*) FROM users WHERE role_code = 'CARRIER' AND status = 'PENDING') AS pendingCarriers,
                (SELECT COUNT(*) FROM users WHERE role_code = 'DRIVER' AND status = 'PENDING') AS pendingDrivers,
                (SELECT COUNT(*) FROM gate_log WHERE in_out_type = 'IN' AND process_result = 'GATE_SUCCESS' AND DATE(entry_time) = CURRENT_DATE) AS todayGateIn,
                (SELECT COUNT(*) FROM gate_log WHERE in_out_type = 'OUT' AND process_result = 'GATE_SUCCESS' AND DATE(exit_time) = CURRENT_DATE) AS todayGateOut,
                (SELECT COUNT(*) FROM plate_recognition) AS recognitionTotal,
                (SELECT COUNT(*) FROM plate_recognition WHERE is_success = true) AS recognitionSuccess,
                (SELECT COUNT(*) FROM plate_recognition WHERE is_success = false) AS recognitionFail,
                (SELECT COUNT(*) FROM exception_log WHERE process_status IS NULL OR process_status <> 'PROCESSED') AS exceptionOpen,
                (SELECT COUNT(*) FROM work_order) AS workTotal,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('DISPATCH_WAITING', 'APPROVED')) AS workReady,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('GATE_IN', 'IN_PROGRESS')) AS workInProgress,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('COMPLETED', 'GATE_OUT')) AS workDone,
                (SELECT COALESCE(SUM(waiting_vehicle_count), 0) FROM sector_metrics) AS waitingVehicles,
                (SELECT COUNT(*) FROM sector_metrics WHERE status_level = 'DANGER') AS congestedSectors,
                (SELECT COUNT(*) FROM sector_metrics WHERE status_level = 'WARNING') AS warningSectors,
                (SELECT COUNT(*) FROM vehicle WHERE vehicle_status = 'MAINTENANCE') AS maintenanceVehicles,
                (SELECT COUNT(*) FROM container WHERE can_exit = false) AS exitHoldContainers
            """)
    DashboardSummaryDTO summary();

    @Select("""
            SELECT
                COALESCE(work_status, 'UNKNOWN') AS workStatus,
                COUNT(*) AS workCount
            FROM work_order
            GROUP BY work_status
            ORDER BY workCount DESC, workStatus ASC
            """)
    List<DashboardWorkStatusDTO> workStatusList();

    @Select("""
            SELECT
                wo.work_order_id AS workOrderId,
                wo.work_type AS workType,
                wo.work_status AS workStatus,
                wo.reserved_time AS reservedTime,
                v.plate_number AS plateNumber,
                d.driver_name AS driverName,
                ca.carrier_name AS carrierName,
                c.container_number AS containerNumber,
                ys.sector_name AS sectorName
            FROM work_order wo
            LEFT JOIN vehicle v ON wo.vehicle_id = v.vehicle_id
            LEFT JOIN driver d ON wo.driver_id = d.driver_id
            LEFT JOIN carrier ca ON d.carrier_id = ca.carrier_id
            LEFT JOIN container c ON wo.container_id = c.container_id
            LEFT JOIN yard_sector ys ON c.sector_id = ys.sector_id
            ORDER BY wo.reserved_time DESC NULLS LAST, wo.work_order_id DESC
            LIMIT 8
            """)
    List<DashboardRecentWorkDTO> recentWorkOrders();

    @Select("""
            SELECT
                ys.sector_id AS sectorId,
                ys.sector_name AS sectorName,
                ys.block_name AS blockName,
                ys.sector_status AS sectorStatus,
                COALESCE(ys.waiting_vehicle_count, 0) AS waitingVehicleCount,
                ys.guide_message AS guideMessage,
                ys.alt_waiting_area AS altWaitingArea,
                40 AS capacity,
                COUNT(DISTINCT c.container_id) AS containerCount,
                ROUND(
                    (COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)) * 100,
                    1
                )::float AS usageRate,
                COUNT(DISTINCT wo.work_order_id) AS workOrderCount,
                CASE
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                    ) >= 0.8
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                      OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN 'DANGER'
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                    ) >= 0.5
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 3
                      OR COUNT(DISTINCT wo.work_order_id) >= 1 THEN 'WARNING'
                    ELSE 'NORMAL'
                END AS statusLevel
            FROM yard_sector ys
            LEFT JOIN container c ON c.sector_id = ys.sector_id
            LEFT JOIN work_order wo
                ON wo.container_id = c.container_id
               AND wo.work_status IN ('DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS')
            GROUP BY
                ys.sector_id,
                ys.sector_name,
                ys.block_name,
                ys.sector_status,
                ys.waiting_vehicle_count,
                ys.guide_message,
                ys.alt_waiting_area
            ORDER BY
                CASE
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                    ) >= 0.8
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                      OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN 1
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(40, 0)
                    ) >= 0.5
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 3
                      OR COUNT(DISTINCT wo.work_order_id) >= 1 THEN 2
                    ELSE 3
                END,
                COALESCE(ys.waiting_vehicle_count, 0) DESC,
                usageRate DESC,
                ys.sector_id ASC
            LIMIT 8
            """)
    List<DashboardSectorDTO> sectorList();
}

