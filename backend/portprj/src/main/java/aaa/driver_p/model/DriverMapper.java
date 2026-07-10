package aaa.driver_p.model;

import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DriverMapper {
    @Select("""
            SELECT
                driver_id,
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter,
                user_id
            FROM driver
            ORDER BY driver_id DESC
            """)
    List<DriverDTO> list();

    @Select("""
            SELECT
                driver_id,
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter,
                user_id
            FROM driver
            WHERE driver_id = #{driverId}
            """)
    DriverDTO detail(Long driverId);

    @Select("""
            SELECT
                driver_id,
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter,
                user_id
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
    @Options(useGeneratedKeys = true, keyProperty = "driverId", keyColumn = "driver_id")
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

    @Delete("""
            DELETE FROM driver
            WHERE driver_id = #{driverId}
            """)
    int delete(Long driverId);

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
    int insertFromRegister(RegisterDTO dto);

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
                ys.guide_message,
                ys.alt_waiting_area
            FROM work_order wo
            LEFT JOIN driver d ON wo.driver_id = d.driver_id
            LEFT JOIN carrier ca ON d.carrier_id = ca.carrier_id
            LEFT JOIN vehicle v ON wo.vehicle_id = v.vehicle_id
            LEFT JOIN container c ON wo.container_id = c.container_id
            LEFT JOIN yard_sector ys ON c.sector_id = ys.sector_id
            WHERE d.driver_name = #{userName}
            ORDER BY wo.reserved_time DESC, wo.work_order_id DESC
            """)
    List<DriverWorkOrderDTO> findWorkOrdersByUserName(String userName);

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
                ys.guide_message,
                ys.alt_waiting_area
            FROM work_order wo
            LEFT JOIN driver d ON wo.driver_id = d.driver_id
            LEFT JOIN carrier ca ON d.carrier_id = ca.carrier_id
            LEFT JOIN vehicle v ON wo.vehicle_id = v.vehicle_id
            LEFT JOIN container c ON wo.container_id = c.container_id
            LEFT JOIN yard_sector ys ON c.sector_id = ys.sector_id
            WHERE d.user_id = #{userId}
            ORDER BY wo.reserved_time DESC, wo.work_order_id DESC
            """)
    List<DriverWorkOrderDTO> findWorkOrdersByUserId(Long userId);
}
