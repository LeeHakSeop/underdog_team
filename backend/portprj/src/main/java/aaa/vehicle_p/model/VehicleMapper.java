package aaa.vehicle_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VehicleMapper {
    @Select("select * from vehicle order by vehicle_id desc")
    List<VehicleDTO> list(VehicleDTO dto);


}
