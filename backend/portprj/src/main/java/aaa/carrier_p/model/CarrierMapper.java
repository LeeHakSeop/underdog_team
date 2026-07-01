package aaa.carrier_p.model;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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
                carrier_status
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
                carrier_status
            FROM carrier
            WHERE carrier_id = #{carrierId}
            """)
    CarrierDTO detail(Long carrierId);

    @Insert("""
            INSERT INTO carrier (
                carrier_name,
                carrier_contact,
                manager_name,
                carrier_status
            ) VALUES (
                #{carrierName},
                #{carrierContact},
                #{managerName},
                #{carrierStatus}
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
                carrier_status = #{carrierStatus}
            WHERE carrier_id = #{carrierId}
            """)
    int update(CarrierDTO dto);

    @Delete("""
            DELETE FROM carrier
            WHERE carrier_id = #{carrierId}
            """)
    int delete(Long carrierId);
}
