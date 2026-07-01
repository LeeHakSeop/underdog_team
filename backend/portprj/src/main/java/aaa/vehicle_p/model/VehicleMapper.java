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
