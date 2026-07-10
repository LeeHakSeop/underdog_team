package aaa.dashboard_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM vehicle) AS totalVehicles,
                (SELECT COUNT(*) FROM users WHERE status = 'PENDING') AS pendingUsers,
                (SELECT COUNT(*) FROM users WHERE role_code = 'CARRIER' AND status = 'PENDING') AS pendingCarriers,
                (SELECT COUNT(*) FROM users WHERE role_code = 'DRIVER' AND status = 'PENDING') AS pendingDrivers,
                (SELECT COUNT(*) FROM gate_log WHERE in_out_type = 'IN' AND DATE(entry_time) = CURRENT_DATE) AS todayGateIn,
                (SELECT COUNT(*) FROM gate_log WHERE in_out_type = 'OUT' AND DATE(exit_time) = CURRENT_DATE) AS todayGateOut,
                (SELECT COUNT(*) FROM plate_recognition) AS recognitionTotal,
                (SELECT COUNT(*) FROM plate_recognition WHERE is_success = true) AS recognitionSuccess,
                (SELECT COUNT(*) FROM plate_recognition WHERE is_success = false) AS recognitionFail,
                (SELECT COUNT(*) FROM exception_log WHERE process_status IS NULL OR process_status <> 'PROCESSED') AS exceptionOpen,
                (SELECT COUNT(*) FROM work_order) AS workTotal,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('DISPATCH_WAITING', 'APPROVED', 'READY', 'WAITING')) AS workReady,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('GATE_IN', 'IN_PROGRESS', 'WORKING')) AS workInProgress,
                (SELECT COUNT(*) FROM work_order WHERE work_status IN ('GATE_OUT', 'DONE', 'COMPLETED')) AS workDone
            """)
    DashboardSummaryDTO summary();

    @Select("""
            SELECT
                COALESCE(work_status, '미지정') AS workStatus,
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
                sector_id AS sectorId,
                sector_name AS sectorName,
                block_name AS blockName,
                sector_status AS sectorStatus,
                waiting_vehicle_count AS waitingVehicleCount,
                guide_message AS guideMessage,
                alt_waiting_area AS altWaitingArea
            FROM yard_sector
            ORDER BY waiting_vehicle_count DESC NULLS LAST, sector_id ASC
            LIMIT 8
            """)
    List<DashboardSectorDTO> sectorList();
}
