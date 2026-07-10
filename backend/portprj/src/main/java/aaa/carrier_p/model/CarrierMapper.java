package aaa.carrier_p.model;

import aaa.auth_p.model.RegisterDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CarrierMapper {

    @Select("""
            SELECT
                carrier_id,
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status,
                user_id
            FROM carrier
            ORDER BY carrier_id DESC
            """)
    List<CarrierDTO> list();

    @Select("""
            SELECT
                carrier_id,
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status,
                user_id
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
    @Options(useGeneratedKeys = true, keyProperty = "carrierId", keyColumn = "carrier_id")
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
