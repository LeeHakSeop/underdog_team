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
                ys.environment_type AS environmentType,
                COALESCE(ys.capacity, 40) AS capacity,
                COALESCE(ys.waiting_vehicle_count, 0) AS waitingVehicleCount,
                ys.guide_message AS guideMessage,
                ys.alt_waiting_area AS altWaitingArea,
                COUNT(DISTINCT c.container_id) AS containerCount,
                ROUND(
                    (COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)) * 100,
                    1
                )::float AS usageRate,
                COUNT(DISTINCT wo.work_order_id) AS workOrderCount,
                CASE
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                    ) >= 0.8
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                      OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN 'DANGER'
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
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
                ys.environment_type,
                ys.capacity,
                ys.waiting_vehicle_count,
                ys.guide_message,
                ys.alt_waiting_area
            ORDER BY ys.sector_id
            """)
    List<YardMapSectorDTO> sectors();

    @Select("""
            WITH normalized_gate_log AS (
                SELECT
                    gate_log_id,
                    vehicle_id,
                    tractor_vehicle_id,
                    trailer_vehicle_id,
                    CASE
                        WHEN gate_number = 'G-IN-01' THEN 'G01'
                        WHEN gate_number = 'G-IN-02' THEN 'G03'
                        WHEN gate_number = 'G-OUT-01' THEN 'G02'
                        WHEN gate_number = 'G-OUT-02' THEN 'G04'
                        WHEN gate_number = 'G01' AND in_out_type = 'OUT' THEN 'G02'
                        WHEN gate_number = 'G03' AND in_out_type = 'OUT' THEN 'G04'
                        ELSE gate_number
                    END AS gate_number,
                    gate_name,
                    entry_time,
                    exit_time,
                    CASE
                        WHEN gate_number IN ('G-IN-01', 'G-IN-02') THEN 'IN'
                        WHEN gate_number IN ('G-OUT-01', 'G-OUT-02') THEN 'OUT'
                        ELSE in_out_type
                    END AS in_out_type,
                    process_result,
                    manager_check
                FROM gate_log
                WHERE gate_number IS NOT NULL
            ),
            latest_gate_log AS (
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
                FROM normalized_gate_log
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
                FROM normalized_gate_log
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
                current_sector.sector_id AS sectorId,
                current_sector.sector_name AS sectorName,
                current_sector.block_name AS blockName,
                CASE
                    WHEN start_sector.sector_name IS NOT NULL THEN start_sector.sector_name
                    WHEN wo.work_status IN ('GATE_IN', 'IN_PROGRESS') THEN COALESCE(gl.gate_name, current_sector.sector_name, '출발 위치 미정')
                    ELSE COALESCE(current_sector.sector_name, '출발 위치 미정')
                END AS originLocation,
                wo.start_sector_id AS startSectorId,
                start_sector.sector_name AS startSectorName,
                wo.destination_sector_id AS destinationSectorId,
                destination_sector.sector_name AS destinationSectorName,
                CONCAT(
                    COALESCE(start_sector.sector_name, current_sector.sector_name, '출발 미정'),
<<<<<<< HEAD
                    ' -> ',
                    COALESCE(destination_sector.sector_name, c.container_location, '목적 미정')
=======
                    ' → ',
                    COALESCE(destination_sector.sector_name, '목적 미정')
>>>>>>> 7d3a4933ce2ffc830bc22fdf80b24cd81fe24306
                ) AS routeSummary,
                wo.reserved_time AS reservedTime
            FROM work_order wo
            LEFT JOIN vehicle vehicle ON vehicle.vehicle_id = wo.vehicle_id
            LEFT JOIN vehicle tractor ON tractor.vehicle_id = wo.tractor_vehicle_id
            LEFT JOIN vehicle trailer ON trailer.vehicle_id = wo.trailer_vehicle_id
            LEFT JOIN driver d ON d.driver_id = wo.driver_id
            LEFT JOIN carrier ca ON ca.carrier_id = d.carrier_id
            LEFT JOIN container c ON c.container_id = wo.container_id
            LEFT JOIN yard_sector current_sector ON current_sector.sector_id = c.sector_id
            LEFT JOIN yard_sector start_sector ON start_sector.sector_id = wo.start_sector_id
            LEFT JOIN yard_sector destination_sector ON destination_sector.sector_id = wo.destination_sector_id
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
