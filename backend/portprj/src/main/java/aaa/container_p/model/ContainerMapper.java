package aaa.container_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContainerMapper {
    @Select("SELECT * FROM container ORDER BY container_id DESC")
    List<ContainerDTO> list();

    @Select("""
            SELECT
                container_id AS containerId,
                container_number AS containerNumber,
                container_size AS containerSize,
                container_location AS containerLocation,
                sector_id AS sectorId,
                block,
                bay,
                row_no AS rowNo,
                can_exit AS canExit,
                seal_number AS sealNumber,
                shipping_line AS shippingLine
            FROM container
            WHERE container_id = #{containerId}
            """)
    ContainerDTO detail(Long containerId);

    @Update("""
            UPDATE container
            SET can_exit = #{canExit}
            WHERE container_id = #{containerId}
            """)
    int updateCanExit(@Param("containerId") Long containerId, @Param("canExit") boolean canExit);
}
