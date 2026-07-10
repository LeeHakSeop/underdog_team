package aaa.vehicle_p.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VehicleMapper {

    @Select("""
<<<<<<< HEAD
            SELECT
                vehicle_id,
                plate_number,
                vehicle_type,
                tonnage,
                is_registered,
                vehicle_status,
                tractor_no,
                chassis_no,
                carrier_id
            FROM vehicle
            WHERE plate_number = #{plateNumber}
            """)
=======
        SELECT
            vehicle_id,
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
        FROM vehicle
        WHERE plate_number = #{plateNumber}
        """)
>>>>>>> origin/main
    VehicleDTO findByPlateNumber(String plateNumber);

    @Select("""
            SELECT
                v.vehicle_id AS vehicleId,
                v.plate_number AS plateNumber,
                v.is_registered AS registeredVehicle,
                CASE WHEN v.is_registered THEN 'YES' ELSE 'NO' END AS registeredText,
                c.carrier_name AS carrierName,
                d.driver_name AS driverName,
                v.vehicle_status AS vehicleStatus,
                v.tractor_no AS tractorNo
            FROM vehicle v
            LEFT JOIN carrier c ON v.carrier_id = c.carrier_id
            LEFT JOIN LATERAL (
                SELECT driver_id
                FROM work_order
                WHERE vehicle_id = v.vehicle_id
                ORDER BY reserved_time DESC NULLS LAST, work_order_id DESC
                LIMIT 1
             ) wo ON true
            LEFT JOIN driver d ON wo.driver_id = d.driver_id
            WHERE v.plate_number = #{plateNumber}
              AND v.vehicle_type = 'TRACTOR'
            """)
    TractorVehicleInfoDTO findTractorInfoByPlateNumber(String plateNumber);

    @Select("""
            SELECT
                vehicle_id,
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
            FROM vehicle
            ORDER BY vehicle_id DESC
            """)
    List<VehicleDTO> list();

    @Select("""
            SELECT
                vehicle_id,
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
    @Options(useGeneratedKeys = true, keyProperty = "vehicleId", keyColumn = "vehicle_id")
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
                vehicle_id,
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
            FROM vehicle
            WHERE driver_id = #{driverId}
            """)
    VehicleDTO findByDriverId(Long driverId);

    @Select("""
            SELECT
                vehicle_id,
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
            FROM vehicle
            WHERE carrier_id = #{carrierId}
            ORDER BY vehicle_id DESC
            """)
    List<VehicleDTO> findByCarrierId(Long carrierId);

    @Delete("""
            DELETE FROM vehicle
            WHERE vehicle_id = #{vehicleId}
            """)
    int delete(Long vehicleId);
}
