package aaa.yard_congestion_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface YardCongestionMapper {

    @Select("""
            WITH sector_metrics AS (
                SELECT
                    ys.sector_id AS sector_id,
                    COALESCE(ys.capacity, 40) AS capacity,
                    COALESCE(ys.waiting_vehicle_count, 0) AS waiting_vehicle_count,
                    COUNT(DISTINCT c.container_id) AS container_count,
                    COUNT(DISTINCT wo.work_order_id) AS work_order_count,
                    ROUND(
                        (COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)) * 100,
                        1
                    )::float AS usage_rate,
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
                    END AS status_level
                FROM yard_sector ys
                LEFT JOIN container c ON c.sector_id = ys.sector_id
                LEFT JOIN work_order wo
                    ON wo.container_id = c.container_id
                   AND wo.work_status IN ('DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS')
                GROUP BY ys.sector_id, ys.capacity, ys.waiting_vehicle_count
            )
            SELECT
                COUNT(*) AS totalSectorCount,
                COUNT(*) FILTER (WHERE status_level = 'NORMAL') AS normalSectorCount,
                COUNT(*) FILTER (WHERE status_level = 'WARNING') AS warningSectorCount,
                COUNT(*) FILTER (WHERE status_level = 'DANGER') AS dangerSectorCount,
                ROUND(AVG(usage_rate)::numeric, 1)::float AS averageUsageRate,
                COALESCE(SUM(capacity), 0) AS totalCapacity,
                COALESCE(SUM(container_count), 0) AS totalContainerCount,
                COALESCE(SUM(waiting_vehicle_count), 0) AS totalWaitingVehicleCount,
                COALESCE(SUM(work_order_count), 0) AS totalWorkOrderCount
            FROM sector_metrics
            """)
    YardCongestionSummaryDTO summary();

    @Select("""
            SELECT
                ys.sector_id AS sectorId,
                ys.sector_name AS sectorName,
                ys.block_name AS blockName,
                ys.sector_status AS sectorStatus,
                COALESCE(ys.capacity, 40) AS capacity,
                COUNT(DISTINCT c.container_id) AS containerCount,
                ROUND(
                    (COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)) * 100,
                    1
                )::float AS usageRate,
                COALESCE(ys.waiting_vehicle_count, 0) AS waitingVehicleCount,
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
                END AS statusLevel,
                CASE
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                    ) >= 0.8
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                      OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN '혼잡'
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                    ) >= 0.5
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 3
                      OR COUNT(DISTINCT wo.work_order_id) >= 1 THEN '주의'
                    ELSE '정상'
                END AS statusLabel,
                CONCAT_WS(
                    ', ',
                    CASE
                        WHEN (
                            COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                        ) >= 0.8 THEN '수용량 80% 이상'
                        WHEN (
                            COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                        ) >= 0.5 THEN '수용량 50% 이상'
                    END,
                    CASE
                        WHEN COALESCE(ys.waiting_vehicle_count, 0) >= 6 THEN '대기 차량 6대 이상'
                        WHEN COALESCE(ys.waiting_vehicle_count, 0) >= 3 THEN '대기 차량 3대 이상'
                    END,
                    CASE
                        WHEN COUNT(DISTINCT wo.work_order_id) >= 3 THEN '진행 작업 3건 이상'
                        WHEN COUNT(DISTINCT wo.work_order_id) >= 1 THEN '진행 작업 있음'
                    END
                ) AS congestionReason,
                ys.guide_message AS guideMessage,
                ys.alt_waiting_area AS altWaitingArea
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
                ys.capacity,
                ys.waiting_vehicle_count,
                ys.guide_message,
                ys.alt_waiting_area
            ORDER BY
                CASE
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                    ) >= 0.8
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 6
                      OR COUNT(DISTINCT wo.work_order_id) >= 3 THEN 1
                    WHEN (
                        COUNT(DISTINCT c.container_id)::numeric / NULLIF(COALESCE(ys.capacity, 40), 0)
                    ) >= 0.5
                      OR COALESCE(ys.waiting_vehicle_count, 0) >= 3
                      OR COUNT(DISTINCT wo.work_order_id) >= 1 THEN 2
                    ELSE 3
                END,
                usageRate DESC,
                COALESCE(ys.waiting_vehicle_count, 0) DESC,
                COUNT(DISTINCT wo.work_order_id) DESC,
                ys.sector_id ASC
            """)
    List<YardCongestionSectorDTO> sectors();
}
