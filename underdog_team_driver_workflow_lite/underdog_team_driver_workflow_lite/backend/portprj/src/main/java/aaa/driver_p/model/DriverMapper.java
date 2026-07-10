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


    @Update("UPDATE vehicle SET driver_id = NULL, user_id = NULL WHERE driver_id = #{driverId} AND UPPER(vehicle_type) IN ('TRAILER', '트레일러')")
    int unassignTrailers(@Param("driverId") Long driverId);

    @Delete("DELETE FROM vehicle WHERE driver_id = #{driverId} AND UPPER(vehicle_type) IN ('TRACTOR', '트랙터')")
    int deleteTractorByDriverId(@Param("driverId") Long driverId);

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
    int insertFromRegister(RegisterDTO dto);


    @Select("""
            SELECT
                d.driver_id AS driverId,
                d.user_id AS userId,
                u.login_id AS loginId,
                u.status AS userStatus,
                d.driver_name AS driverName,
                d.driver_contact AS driverContact,
                d.is_registered AS isRegistered,
                d.can_enter AS canEnter,
                c.carrier_id AS carrierId,
                c.carrier_name AS carrierName,
                v.vehicle_id AS vehicleId,
                v.plate_number AS plateNumber,
                v.vehicle_type AS vehicleType,
                v.tonnage AS tonnage,
                v.is_registered AS vehicleRegistered,
                v.vehicle_status AS vehicleStatus,
                v.tractor_no AS tractorNo,
                v.chassis_no AS chassisNo
            FROM carrier c
            INNER JOIN driver d
                ON d.carrier_id = c.carrier_id
            LEFT JOIN users u
                ON u.user_id = d.user_id
            LEFT JOIN vehicle v
                ON v.driver_id = d.driver_id
               AND UPPER(v.vehicle_type) IN ('TRACTOR', '트랙터')
            WHERE c.user_id = #{carrierUserId}
            ORDER BY d.is_registered ASC,
                     d.driver_name ASC,
                     d.driver_id DESC
            """)
    List<CarrierDriverManagementDTO> findManagementListByCarrierUserId(
            @Param("carrierUserId") Long carrierUserId
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
                ys.guide_message,
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