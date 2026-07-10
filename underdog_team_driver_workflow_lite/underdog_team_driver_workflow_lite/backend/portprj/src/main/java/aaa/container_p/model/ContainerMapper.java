package aaa.container_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContainerMapper {
    @Select("SELECT * FROM container ORDER BY container_id DESC")
    List<ContainerDTO> list();
}
