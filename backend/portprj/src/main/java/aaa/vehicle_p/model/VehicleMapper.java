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
    VehicleDTO findByPlateNumber(String plateNumber);

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
    VehicleDTO detail(Long vehicleId);

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

    @Delete("""
        DELETE FROM vehicle
        WHERE vehicle_id = #{vehicleId}
        """)
    int delete(Long vehicleId);

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
        """)
    VehicleDTO findByDriverId(Long driverId);

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
    List<VehicleDTO> findByCarrierId(Long carrierId);
}