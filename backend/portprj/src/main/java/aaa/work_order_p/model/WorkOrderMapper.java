package aaa.work_order_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkOrderMapper {
    @Select("""
            SELECT
                work_order_id AS workOrderId,
                work_type AS workType,
                vehicle_id AS vehicleId,
                tractor_vehicle_id AS tractorVehicleId,
                trailer_vehicle_id AS trailerVehicleId,
                driver_id AS driverId,
                container_id AS containerId,
                reserved_time AS reservedTime,
                work_status AS workStatus,
                is_approved AS isApproved
            FROM work_order
            ORDER BY work_order_id DESC
            """)
    List<WorkOrderDTO> list();

    @Select("""
            SELECT
                work_order_id AS workOrderId,
                work_type AS workType,
                vehicle_id AS vehicleId,
                tractor_vehicle_id AS tractorVehicleId,
                trailer_vehicle_id AS trailerVehicleId,
                driver_id AS driverId,
                container_id AS containerId,
                reserved_time AS reservedTime,
                work_status AS workStatus,
                is_approved AS isApproved
            FROM work_order
            WHERE work_order_id = #{workOrderId}
            """)
    WorkOrderDTO detail(Long workOrderId);

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

    @Select("""
            SELECT
                wo.work_order_id AS workOrderId,
                wo.work_type AS workType,
                wo.vehicle_id AS vehicleId,
                wo.tractor_vehicle_id AS tractorVehicleId,
                wo.trailer_vehicle_id AS trailerVehicleId,
                wo.driver_id AS driverId,
                wo.container_id AS containerId,
                wo.work_status AS workStatus,
                wo.is_approved AS isApproved,
                c.container_number AS containerNumber,
                c.container_location AS containerLocation,
                CONCAT(c.block, '-', c.bay, '-', c.row_no) AS yardLocation,
                c.block AS block,
                c.bay AS bay,
                c.row_no AS rowNo,
                c.sector_id AS sectorId,
                ys.sector_name AS sectorName,
                ys.block_name AS blockName,
                ys.sector_status AS sectorStatus,
                CASE
                    WHEN wo.work_status = 'DISPATCH_WAITING'
                        THEN '운송사 배차 승인 대기 중입니다.'
                    WHEN wo.work_status = 'APPROVED'
                        THEN '입차 승인 완료. 게이트에서 입차 처리 후 야드 섹터로 이동하세요.'
                    WHEN wo.work_status = 'GATE_IN'
                        THEN CONCAT('입차 처리 완료. ', COALESCE(ys.sector_name, c.container_location, '지정된 야드 섹터'), ' 섹터로 이동하여 작업을 진행하세요.')
                    WHEN wo.work_status = 'IN_PROGRESS'
                        THEN CONCAT('작업 진행 중입니다. ', COALESCE(ys.sector_name, c.container_location, '지정된 야드 섹터'), ' 섹터의 작업 위치를 확인하세요.')
                    WHEN wo.work_status = 'COMPLETED' AND COALESCE(c.can_exit, FALSE) = TRUE
                        THEN '작업 완료 및 출차 가능 상태입니다. 게이트에서 출차 처리하세요.'
                    WHEN wo.work_status = 'COMPLETED'
                        THEN '작업 완료. 출차 보류 및 관리자 확인 대기 중입니다.'
                    WHEN wo.work_status = 'GATE_OUT'
                        THEN '출차 처리가 완료되었습니다.'
                    WHEN wo.work_status = 'CANCELED'
                        THEN '취소된 작업지시입니다.'
                    ELSE COALESCE(ys.guide_message, '작업 상태를 확인하세요.')
                END AS guideMessage,
                c.can_exit AS canExit,
                ys.alt_waiting_area AS altWaitingArea,
                CONCAT(
                    '컨테이너 번호는 ', c.container_number,
                    '이고, 작업 유형은 ', wo.work_type,
                    '입니다. 이동 위치는 ', COALESCE(ys.sector_name, c.container_location),
                    ' / ', COALESCE(c.block, '-'), '-', COALESCE(c.bay, '-'), '-', COALESCE(c.row_no, '-'),
                    CONCAT(
                        '. 안내: ',
                        CASE
                            WHEN wo.work_status = 'DISPATCH_WAITING'
                                THEN '운송사 배차 승인 대기 중입니다.'
                            WHEN wo.work_status = 'APPROVED'
                                THEN '입차 승인 완료. 게이트에서 입차 처리 후 야드 섹터로 이동하세요.'
                            WHEN wo.work_status = 'GATE_IN'
                                THEN CONCAT('입차 처리 완료. ', COALESCE(ys.sector_name, c.container_location, '지정된 야드 섹터'), ' 섹터로 이동하여 작업을 진행하세요.')
                            WHEN wo.work_status = 'IN_PROGRESS'
                                THEN CONCAT('작업 진행 중입니다. ', COALESCE(ys.sector_name, c.container_location, '지정된 야드 섹터'), ' 섹터의 작업 위치를 확인하세요.')
                            WHEN wo.work_status = 'COMPLETED' AND COALESCE(c.can_exit, FALSE) = TRUE
                                THEN '작업 완료 및 출차 가능 상태입니다. 게이트에서 출차 처리하세요.'
                            WHEN wo.work_status = 'COMPLETED'
                                THEN '작업 완료. 출차 보류 및 관리자 확인 대기 중입니다.'
                            WHEN wo.work_status = 'GATE_OUT'
                                THEN '출차 처리가 완료되었습니다.'
                            WHEN wo.work_status = 'CANCELED'
                                THEN '취소된 작업지시입니다.'
                            ELSE COALESCE(ys.guide_message, '작업 상태를 확인하세요.')
                        END
                    ),
                    CASE
                        WHEN ys.alt_waiting_area IS NOT NULL AND ys.alt_waiting_area <> ''
                        THEN CONCAT('. 대체 대기 위치: ', ys.alt_waiting_area)
                        ELSE ''
                    END,
                    '입니다.'
                ) AS workGuideMessage
            FROM work_order wo
            JOIN container c ON wo.container_id = c.container_id
            JOIN yard_sector ys ON c.sector_id = ys.sector_id
            WHERE (wo.trailer_vehicle_id = #{vehicleId}
               OR wo.vehicle_id = #{vehicleId})
              AND COALESCE(wo.is_approved, FALSE) = TRUE
              AND wo.work_status IN ('APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED')
            ORDER BY wo.reserved_time DESC NULLS LAST, wo.work_order_id DESC
            LIMIT 1
            """)
    TrailerWorkInfoDTO findTrailerWorkInfoByVehicleId(Long vehicleId);

    @Select("""
            SELECT
                work_order_id AS workOrderId,
                work_type AS workType,
                vehicle_id AS vehicleId,
                tractor_vehicle_id AS tractorVehicleId,
                trailer_vehicle_id AS trailerVehicleId,
                driver_id AS driverId,
                container_id AS containerId,
                reserved_time AS reservedTime,
                work_status AS workStatus,
                is_approved AS isApproved
            FROM work_order
            WHERE (tractor_vehicle_id = #{vehicleId}
               OR vehicle_id = #{vehicleId})
              AND COALESCE(is_approved, FALSE) = TRUE
              AND work_status IN ('APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED')
            ORDER BY reserved_time DESC NULLS LAST, work_order_id DESC
            LIMIT 1
            """)
    WorkOrderDTO findLatestByTractorVehicleId(Long vehicleId);

    @Select("""
            SELECT
                work_order_id AS workOrderId,
                work_type AS workType,
                vehicle_id AS vehicleId,
                tractor_vehicle_id AS tractorVehicleId,
                trailer_vehicle_id AS trailerVehicleId,
                driver_id AS driverId,
                container_id AS containerId,
                reserved_time AS reservedTime,
                work_status AS workStatus,
                is_approved AS isApproved
            FROM work_order
            WHERE (trailer_vehicle_id = #{vehicleId}
               OR vehicle_id = #{vehicleId})
              AND COALESCE(is_approved, FALSE) = TRUE
              AND work_status IN ('APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED')
            ORDER BY reserved_time DESC NULLS LAST, work_order_id DESC
            LIMIT 1
            """)
    WorkOrderDTO findLatestByTrailerVehicleId(Long vehicleId);

    @Update("""
            UPDATE work_order
            SET work_status = #{workStatus}
            WHERE work_order_id = #{workOrderId}
            """)
    int updateStatus(@Param("workOrderId") Long workOrderId, @Param("workStatus") String workStatus);

    @Update("""
            UPDATE work_order
            SET is_approved = true,
                work_status = 'APPROVED'
            WHERE work_order_id = #{workOrderId}
            """)
    int approve(Long workOrderId);

    @Update("""
            UPDATE work_order
            SET is_approved = false,
                work_status = 'CANCELED'
            WHERE work_order_id = #{workOrderId}
            """)
    int reject(Long workOrderId);
}
