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
    DriverDTO detail(Long driverId);

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
            WHERE user_id = #{userId}
            """)
    int updateApprovalByUserId(
            @Param("userId") Long userId,
            @Param("isRegistered") boolean isRegistered,
            @Param("canEnter") boolean canEnter
    );
}