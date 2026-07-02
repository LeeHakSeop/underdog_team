package aaa.yard_sector_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface YardSectorMapper {
    @Select("SELECT * FROM yard_sector ORDER BY sector_id")
    List<YardSectorDTO> list();
}
