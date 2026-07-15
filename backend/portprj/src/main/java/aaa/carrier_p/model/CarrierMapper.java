package aaa.carrier_p.model;

import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CarrierMapper {

    @Select("""
            SELECT
                carrier_id AS carrierId,
                carrier_name AS carrierName,
                carrier_contact AS carrierContact,
                manager_name AS managerName,
                carrier_status AS carrierStatus,
                user_id AS userId
            FROM carrier
            ORDER BY carrier_id DESC
            """)
    List<CarrierDTO> list();

    @Select("""
            SELECT
                carrier_id AS carrierId,
                carrier_name AS carrierName,
                carrier_contact AS carrierContact,
                manager_name AS managerName,
                carrier_status AS carrierStatus,
                user_id AS userId
            FROM carrier
            WHERE carrier_id = #{carrierId}
            """)
    CarrierDTO detail(Long carrierId);

    @Insert("""
            INSERT INTO carrier (
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status,
                user_id
            ) VALUES (
                #{carrierName},
                #{carrierContact},
                #{managerName},
                #{carrierStatus},
                #{userId}
            )
            """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "carrierId",
            keyColumn = "carrier_id"
    )
    int insert(CarrierDTO dto);

    @Update("""
            UPDATE carrier
            SET
                carrier_name = #{carrierName},
                carrier_contact = #{carrierContact},
                manager_name = #{managerName},
                carrier_status = #{carrierStatus},
                user_id = #{userId}
            WHERE carrier_id = #{carrierId}
            """)
    int update(CarrierDTO dto);

    @Delete("""
        DELETE FROM carrier
        WHERE carrier_id = #{carrierId}
        """)
    int delete(Long carrierId);

    @Select("""
        SELECT user_id
        FROM driver
        WHERE carrier_id = #{carrierId}
          AND user_id IS NOT NULL
        """)
    List<Long> findDriverUserIds(@Param("carrierId") Long carrierId);

    @Update("""
        UPDATE work_order
        SET driver_id = NULL
        WHERE driver_id IN (
            SELECT driver_id
            FROM driver
            WHERE carrier_id = #{carrierId}
        )
        """)
    int clearWorkOrderDriverReferences(@Param("carrierId") Long carrierId);

    @Update("""
        UPDATE vehicle
        SET carrier_id = NULL,
            driver_id = NULL,
            user_id = NULL
        WHERE carrier_id = #{carrierId}
        """)
    int detachVehicles(@Param("carrierId") Long carrierId);

    @Delete("""
        DELETE FROM driver
        WHERE carrier_id = #{carrierId}
        """)
    int deleteDrivers(@Param("carrierId") Long carrierId);

    @Insert("""
            INSERT INTO carrier (
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status,
                user_id
            ) VALUES (
                #{carrierName},
                #{carrierContact},
                #{managerName},
                'PENDING',
                #{userId}
            )
            """)
    int insertFromRegister(RegisterDTO dto);

    @Update("""
            UPDATE carrier
            SET carrier_status = #{status}
            WHERE user_id = #{userId}
            """)
    int updateStatusByUserId(
            @Param("userId") Long userId,
            @Param("status") String status
    );
}
