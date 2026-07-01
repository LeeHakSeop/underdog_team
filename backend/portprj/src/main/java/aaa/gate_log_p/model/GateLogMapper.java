package aaa.gate_log_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GateLogMapper {
    @Select("SELECT * FROM gate_log ORDER BY gate_log_id DESC")
    List<GateLogDTO> list();
}
