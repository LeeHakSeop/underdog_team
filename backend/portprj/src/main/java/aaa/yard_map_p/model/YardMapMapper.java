package aaa.yard_map_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface YardMapMapper {

    @Select("""
            SELECT
                ys.sector_id AS sectorId,
                ys.sector_name AS sectorName,
                ys.block_name AS blockName,
                ys.sector_status AS sectorStatus,
                40 AS capacity,
                COALESCE(ys.waiting_vehicle_count, 0) AS waitingVehicleCount,
                ys.guide_message AS guideMessage,
                ys.alt_waiting_area AS altWaitingArea,
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
            ORDER BY ys.sector_id
            """)
    List<YardMapSectorDTO> sectors();

    @Select("""
            WITH latest_gate_log AS (
                SELECT DISTINCT ON (gate_number)
                    gate_log_id,
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
                FROM gate_log
                WHERE gate_number IS NOT NULL
                ORDER BY gate_number, COALESCE(exit_time, entry_time) DESC NULLS LAST, gate_log_id DESC
            ),
            gate_counts AS (
                SELECT
                    gate_number,
                    COUNT(*) FILTER (
                        WHERE in_out_type = 'IN'
                          AND DATE(COALESCE(entry_time, exit_time)) = CURRENT_DATE
                    ) AS todayInCount,
                    COUNT(*) FILTER (
                        WHERE in_out_type = 'OUT'
                          AND DATE(COALESCE(exit_time, entry_time)) = CURRENT_DATE
                    ) AS todayOutCount
                FROM gate_log
                WHERE gate_number IS NOT NULL
                GROUP BY gate_number
            )
            SELECT
                l.gate_number AS gateNumber,
                l.gate_name AS gateName,
                l.in_out_type AS direction,
                l.gate_log_id AS latestGateLogId,
                l.vehicle_id AS latestVehicleId,
                l.tractor_vehicle_id AS latestTractorVehicleId,
                l.trailer_vehicle_id AS latestTrailerVehicleId,
                l.entry_time AS latestEntryTime,
                l.exit_time AS latestExitTime,
                l.process_result AS latestProcessResult,
                l.manager_check AS managerCheck,
                COALESCE(c.todayInCount, 0) AS todayInCount,
                COALESCE(c.todayOutCount, 0) AS todayOutCount
            FROM latest_gate_log l
            LEFT JOIN gate_counts c ON c.gate_number = l.gate_number
            ORDER BY l.gate_number
            """)
    List<YardMapGateDTO> gates();

    @Select("""
            SELECT
                wo.work_order_id AS workOrderId,
                wo.work_type AS workType,
                wo.work_status AS workStatus,
                wo.vehicle_id AS vehicleId,
                wo.tractor_vehicle_id AS tractorVehicleId,
                wo.trailer_vehicle_id AS trailerVehicleId,
                COALESCE(tractor.plate_number, vehicle.plate_number) AS tractorPlateNumber,
                trailer.plate_number AS trailerPlateNumber,
                COALESCE(tractor.vehicle_status, vehicle.vehicle_status) AS tractorVehicleStatus,
                trailer.vehicle_status AS trailerVehicleStatus,
                c.container_id AS containerId,
                c.container_number AS containerNumber,
                c.container_size AS containerSize,
                c.container_location AS containerLocation,
                c.can_exit AS canExit,
                d.driver_name AS driverName,
                ca.carrier_name AS carrierName,
                ys.sector_id AS sectorId,
                ys.sector_name AS sectorName,
                ys.block_name AS blockName,
                CASE
                    WHEN wo.work_status IN ('DISPATCH_WAITING', 'APPROVED') THEN '입차 게이트'
                    WHEN wo.work_status IN ('GATE_IN', 'IN_PROGRESS') THEN COALESCE(gl.gate_name, '입차 게이트')
                    ELSE '운영 위치 미정'
                END AS originLocation,
                ys.sector_id AS destinationSectorId,
                ys.sector_name AS destinationSectorName,
                CONCAT(
                    CASE
                        WHEN wo.work_status IN ('DISPATCH_WAITING', 'APPROVED') THEN '입차 게이트'
                        WHEN wo.work_status IN ('GATE_IN', 'IN_PROGRESS') THEN COALESCE(gl.gate_name, '입차 게이트')
                        ELSE '운영 위치 미정'
                    END,
                    ' → ',
                    COALESCE(ys.sector_name, c.container_location, '목적 섹터 미정')
                ) AS routeSummary,
                wo.reserved_time AS reservedTime
            FROM work_order wo
            LEFT JOIN vehicle vehicle ON vehicle.vehicle_id = wo.vehicle_id
            LEFT JOIN vehicle tractor ON tractor.vehicle_id = wo.tractor_vehicle_id
            LEFT JOIN vehicle trailer ON trailer.vehicle_id = wo.trailer_vehicle_id
            LEFT JOIN driver d ON d.driver_id = wo.driver_id
            LEFT JOIN carrier ca ON ca.carrier_id = d.carrier_id
            LEFT JOIN container c ON c.container_id = wo.container_id
            LEFT JOIN yard_sector ys ON ys.sector_id = c.sector_id
            LEFT JOIN LATERAL (
                SELECT gate_name
                FROM gate_log
                WHERE gate_log.vehicle_id IN (wo.vehicle_id, wo.tractor_vehicle_id)
                   OR gate_log.tractor_vehicle_id IN (wo.vehicle_id, wo.tractor_vehicle_id)
                ORDER BY COALESCE(entry_time, exit_time) DESC NULLS LAST, gate_log_id DESC
                LIMIT 1
            ) gl ON TRUE
            WHERE wo.work_status IN ('DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS')
            ORDER BY wo.reserved_time DESC NULLS LAST, wo.work_order_id DESC
            LIMIT 120
            """)
    List<YardMapVehicleDTO> vehicles();
}

