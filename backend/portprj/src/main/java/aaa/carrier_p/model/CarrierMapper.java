package aaa.carrier_p.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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
}
