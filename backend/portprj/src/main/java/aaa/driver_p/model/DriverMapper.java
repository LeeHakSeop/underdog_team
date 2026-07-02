package aaa.driver_p.model;

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
                can_enter
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
                can_enter
            FROM driver
            WHERE driver_id = #{driverId}
            """)
    DriverDTO detail(Long driverId);

    @Insert("""
            INSERT INTO driver (
                driver_name,
                driver_contact,
                is_registered,
                carrier_id,
                can_enter
            ) VALUES (
                #{driverName},
                #{driverContact},
                #{isRegistered},
                #{carrierId},
                #{canEnter}
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
                can_enter = #{canEnter}
            WHERE driver_id = #{driverId}
            """)
    int update(DriverDTO dto);

    @Delete("""
            DELETE FROM driver
            WHERE driver_id = #{driverId}
            """)
    int delete(Long driverId);
}
