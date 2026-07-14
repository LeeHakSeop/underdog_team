package aaa.driver_p.model;

import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DriverMapper {

    @Select("""
            SELECT
                driver_id AS driverId,
                driver_name AS driverName,
                driver_contact AS driverContact,
                is_registered AS isRegistered,
                carrier_id AS carrierId,
                can_enter AS canEnter,
                user_id AS userId
            FROM driver
            ORDER BY driver_id DESC
            """)
    List<DriverDTO> list();

    @Select("""
            SELECT
                driver_id AS driverId,
                driver_name AS driverName,
                driver_contact AS driverContact,
                is_registered AS isRegistered,
                carrier_id AS carrierId,
                can_enter AS canEnter,
                user_id AS userId
            FROM driver
            WHERE driver_id = #{driverId}
            """)
    DriverDTO detail(@Param("driverId") Long driverId);

    @Select("""
            SELECT
                driver_id AS driverId,
                driver_name AS driverName,
                driver_contact AS driverContact,
                is_registered AS isRegistered,
                carrier_id AS carrierId,
                can_enter AS canEnter,
                user_id AS userId
            FROM driver
            WHERE user_id = #{userId}
            """)
    DriverDTO findByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO driver (
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter,
                user_id
            ) VALUES (
                #{driverName},
                #{driverContact},
                #{isRegistered},
                #{carrierId},
                #{canEnter},
                #{userId}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "driverId",
            keyColumn = "driver_id"
    )
    int insert(DriverDTO dto);

    @Update("""
            UPDATE driver
            SET
                driver_name = #{driverName},
                driver_contact = #{driverContact},
                is_registered = #{isRegistered},
                carrier_id = #{carrierId},
                can_enter = #{canEnter},
                user_id = #{userId}
            WHERE driver_id = #{driverId}
            """)
    int update(DriverDTO dto);

    @Update("""
            UPDATE driver
            SET
                is_registered = #{isRegistered},
                can_enter = #{canEnter}
            WHERE driver_id = #{driverId}
            """)
    int updateApprovalByDriverId(
            @Param("driverId") Long driverId,
            @Param("isRegistered") boolean isRegistered,
            @Param("canEnter") boolean canEnter
    );

    @Update("""
            UPDATE driver
            SET
                is_registered = #{isRegistered},
                can_enter = #{canEnter}
            WHERE user_id = #{userId}
            """)
    int updateApprovalByUserId(
            @Param("userId") Long userId,
            @Param("isRegistered") boolean isRegistered,
            @Param("canEnter") boolean canEnter
    );

    @Delete("""
            DELETE FROM driver
            WHERE driver_id = #{driverId}
            """)
    int delete(@Param("driverId") Long driverId);

    @Insert("""
            INSERT INTO driver (
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter,
                user_id
            ) VALUES (
                #{driverName},
                #{driverContact},
                false,
                #{carrierId},
                false,
                #{userId}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "driverId",
            keyColumn = "driver_id"
    )
    int insertFromRegister(RegisterDTO dto);

    @Select("""
            SELECT
                wo.work_order_id,
                wo.work_type,
                wo.reserved_time,
                wo.work_status,
                wo.is_approved,
                d.driver_id,
                d.driver_name,
                d.driver_contact,
                d.can_enter,
                ca.carrier_id,
                ca.carrier_name,
                ca.carrier_contact,
                v.vehicle_id,
                v.plate_number,
                v.vehicle_type,
                v.vehicle_status,
                c.container_id,
                c.container_number,
                c.container_size,
                c.container_location,
                c.block,
                c.bay,
                c.row_no,
                ys.sector_id,
                ys.sector_name,
                ys.sector_status,
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
                END AS guide_message,
                c.can_exit AS can_exit,
                ys.alt_waiting_area
            FROM work_order wo
            LEFT JOIN driver d
                ON wo.driver_id = d.driver_id
            LEFT JOIN carrier ca
                ON d.carrier_id = ca.carrier_id
            LEFT JOIN vehicle v
                ON wo.vehicle_id = v.vehicle_id
            LEFT JOIN container c
                ON wo.container_id = c.container_id
            LEFT JOIN yard_sector ys
                ON c.sector_id = ys.sector_id
            WHERE d.driver_name = #{userName}
            ORDER BY wo.reserved_time DESC, wo.work_order_id DESC
            """)
    List<DriverWorkOrderDTO> findWorkOrdersByUserName(
            @Param("userName") String userName
    );

    @Select("""
            SELECT
                wo.work_order_id,
                wo.work_type,
                wo.reserved_time,
                wo.work_status,
                wo.is_approved,
                d.driver_id,
                d.driver_name,
                d.driver_contact,
                d.can_enter,
                ca.carrier_id,
                ca.carrier_name,
                ca.carrier_contact,
                v.vehicle_id,
                v.plate_number,
                v.vehicle_type,
                v.vehicle_status,
                c.container_id,
                c.container_number,
                c.container_size,
                c.container_location,
                c.block,
                c.bay,
                c.row_no,
                ys.sector_id,
                ys.sector_name,
                ys.sector_status,
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
                END AS guide_message,
                c.can_exit AS can_exit,
                ys.alt_waiting_area
            FROM work_order wo
            LEFT JOIN driver d
                ON wo.driver_id = d.driver_id
            LEFT JOIN carrier ca
                ON d.carrier_id = ca.carrier_id
            LEFT JOIN vehicle v
                ON wo.vehicle_id = v.vehicle_id
            LEFT JOIN container c
                ON wo.container_id = c.container_id
            LEFT JOIN yard_sector ys
                ON c.sector_id = ys.sector_id
            WHERE d.user_id = #{userId}
            ORDER BY wo.reserved_time DESC, wo.work_order_id DESC
            """)
    List<DriverWorkOrderDTO> findWorkOrdersByUserId(
            @Param("userId") Long userId
    );
}
