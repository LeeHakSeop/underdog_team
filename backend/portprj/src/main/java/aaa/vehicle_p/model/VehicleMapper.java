package aaa.vehicle_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

    @Select("""
        SELECT
            vehicle_id AS vehicleId,
            plate_number AS plateNumber,
            vehicle_type AS vehicleType,
            tonnage,
            is_registered AS isRegistered,
            vehicle_status AS vehicleStatus,
            tractor_no AS tractorNo,
            chassis_no AS chassisNo,
            driver_id AS driverId,
            carrier_id AS carrierId,
            user_id AS userId
        FROM vehicle
        WHERE plate_number = #{plateNumber}
        """)
    VehicleDTO findByPlateNumber(@Param("plateNumber") String plateNumber);

    @Select("""
        SELECT
            v.vehicle_id AS vehicleId,
            v.plate_number AS plateNumber,
            v.is_registered AS registeredVehicle,
            CASE
                WHEN v.is_registered THEN '예'
                ELSE '아니오'
            END AS registeredText,
            c.carrier_name AS carrierName,
            d.driver_name AS driverName,
            v.vehicle_status AS vehicleStatus,
            v.tractor_no AS tractorNo
        FROM vehicle v
        LEFT JOIN carrier c
            ON v.carrier_id = c.carrier_id
        LEFT JOIN LATERAL (
            SELECT driver_id
            FROM work_order
            WHERE (tractor_vehicle_id = v.vehicle_id
               OR vehicle_id = v.vehicle_id)
              AND COALESCE(is_approved, FALSE) = TRUE
              AND work_status IN ('APPROVED', 'GATE_IN', 'IN_PROGRESS', 'COMPLETED')
            ORDER BY reserved_time DESC NULLS LAST,
                     work_order_id DESC
            LIMIT 1
        ) wo ON true
        LEFT JOIN driver d
            ON wo.driver_id = d.driver_id
        WHERE v.plate_number = #{plateNumber}
          AND v.vehicle_type IN ('TRACTOR', '트랙터')
        """)
    TractorVehicleInfoDTO findTractorInfoByPlateNumber(
            @Param("plateNumber") String plateNumber
    );

    @Select("""
        SELECT
            vehicle_id AS vehicleId,
            plate_number AS plateNumber,
            vehicle_type AS vehicleType,
            tonnage,
            is_registered AS isRegistered,
            vehicle_status AS vehicleStatus,
            tractor_no AS tractorNo,
            chassis_no AS chassisNo,
            driver_id AS driverId,
            carrier_id AS carrierId,
            user_id AS userId
        FROM vehicle
        ORDER BY vehicle_id DESC
        """)
    List<VehicleDTO> list();

    @Select("""
        SELECT
            vehicle_id AS vehicleId,
            plate_number AS plateNumber,
            vehicle_type AS vehicleType,
            tonnage,
            is_registered AS isRegistered,
            vehicle_status AS vehicleStatus,
            tractor_no AS tractorNo,
            chassis_no AS chassisNo,
            driver_id AS driverId,
            carrier_id AS carrierId,
            user_id AS userId
        FROM vehicle
        WHERE vehicle_id = #{vehicleId}
        """)
    VehicleDTO detail(@Param("vehicleId") Long vehicleId);

    @Insert("""
        INSERT INTO vehicle (
            plate_number,
            vehicle_type,
            tonnage,
            is_registered,
            vehicle_status,
            tractor_no,
            chassis_no,
            driver_id,
            carrier_id,
            user_id
        ) VALUES (
            #{plateNumber},
            #{vehicleType},
            #{tonnage},
            #{isRegistered},
            #{vehicleStatus},
            #{tractorNo},
            #{chassisNo},
            #{driverId},
            #{carrierId},
            #{userId}
        )
        """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "vehicleId",
            keyColumn = "vehicle_id"
    )
    int insert(VehicleDTO dto);

    @Update("""
        UPDATE vehicle
        SET
            plate_number = #{plateNumber},
            vehicle_type = #{vehicleType},
            tonnage = #{tonnage},
            is_registered = #{isRegistered},
            vehicle_status = #{vehicleStatus},
            tractor_no = #{tractorNo},
            chassis_no = #{chassisNo},
            driver_id = #{driverId},
            carrier_id = #{carrierId},
            user_id = #{userId}
        WHERE vehicle_id = #{vehicleId}
        """)
    int update(VehicleDTO dto);

    @Update("""
        UPDATE vehicle
        SET
            is_registered = #{isRegistered},
            vehicle_status = #{vehicleStatus}
        WHERE vehicle_id = #{vehicleId}
        """)
    int updateApproval(
            @Param("vehicleId") Long vehicleId,
            @Param("isRegistered") boolean isRegistered,
            @Param("vehicleStatus") String vehicleStatus
    );

    @Select("""
        SELECT
            vehicle_id AS vehicleId,
            plate_number AS plateNumber,
            vehicle_type AS vehicleType,
            tonnage,
            is_registered AS isRegistered,
            vehicle_status AS vehicleStatus,
            tractor_no AS tractorNo,
            chassis_no AS chassisNo,
            driver_id AS driverId,
            carrier_id AS carrierId,
            user_id AS userId
        FROM vehicle
        WHERE driver_id = #{driverId}
          AND vehicle_type IN ('TRACTOR', '트랙터')
        ORDER BY vehicle_id DESC
        LIMIT 1
        """)
    VehicleDTO findByDriverId(@Param("driverId") Long driverId);

    @Select("""
        SELECT
            vehicle_id AS vehicleId,
            plate_number AS plateNumber,
            vehicle_type AS vehicleType,
            tonnage,
            is_registered AS isRegistered,
            vehicle_status AS vehicleStatus,
            tractor_no AS tractorNo,
            chassis_no AS chassisNo,
            driver_id AS driverId,
            carrier_id AS carrierId,
            user_id AS userId
        FROM vehicle
        WHERE carrier_id = #{carrierId}
        ORDER BY vehicle_id DESC
        """)
    List<VehicleDTO> findByCarrierId(@Param("carrierId") Long carrierId);

    @Delete("""
        DELETE FROM vehicle
        WHERE vehicle_id = #{vehicleId}
        """)
    int delete(@Param("vehicleId") Long vehicleId);
}
