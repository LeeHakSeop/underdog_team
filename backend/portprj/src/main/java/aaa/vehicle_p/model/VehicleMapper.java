package aaa.vehicle_p.model;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {

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
            carrier_id
        FROM vehicle
        WHERE plate_number = #{plateNumber}
        """)
    VehicleDTO findByPlateNumber(String plateNumber);

    @Select("""
            SELECT
                v.vehicle_id AS vehicleId,
                v.plate_number AS plateNumber,
                v.is_registered AS registeredVehicle,
                CASE WHEN v.is_registered THEN '예' ELSE '아니오' END AS registeredText,
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
                carrier_id
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
                carrier_id
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
                carrier_id
            ) VALUES (
                #{plateNumber},
                #{vehicleType},
                #{tonnage},
                #{isRegistered},
                #{vehicleStatus},
                #{tractorNo},
                #{chassisNo},
                #{carrierId}
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
                carrier_id = #{carrierId}
            WHERE vehicle_id = #{vehicleId}
            """)
    int update(VehicleDTO dto);

    @Delete("""
            DELETE FROM vehicle
            WHERE vehicle_id = #{vehicleId}
            """)
    int delete(Long vehicleId);
}
