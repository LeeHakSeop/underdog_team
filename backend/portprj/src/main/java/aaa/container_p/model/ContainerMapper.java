package aaa.container_p.model;

import org.apache.ibatis.annotations.*;

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
            WHERE container_number = #{containerNumber}
            """)
    ContainerDTO findByContainerNumber(@Param("containerNumber") String containerNumber);

    @Insert("""
            INSERT INTO container (
                container_number, container_size, container_location, sector_id,
                block, bay, row_no, can_exit, seal_number, shipping_line
            ) VALUES (
                #{containerNumber}, #{containerSize}, #{containerLocation}, #{sectorId},
                #{block}, #{bay}, #{rowNo}, #{canExit}, #{sealNumber}, #{shippingLine}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "containerId", keyColumn = "container_id")
    int insert(ContainerDTO dto);

    @Update("""
            UPDATE container
            SET container_number = #{containerNumber},
                container_size = #{containerSize},
                container_location = #{containerLocation},
                sector_id = #{sectorId},
                block = #{block},
                bay = #{bay},
                row_no = #{rowNo},
                can_exit = #{canExit},
                seal_number = #{sealNumber},
                shipping_line = #{shippingLine}
            WHERE container_id = #{containerId}
            """)
    int update(ContainerDTO dto);

    @Delete("""
            DELETE FROM container
            WHERE container_id = #{containerId}
            """)
    int delete(@Param("containerId") Long containerId);

    @Select("""
            SELECT COUNT(*)
            FROM work_order
            WHERE container_id = #{containerId}
            """)
    int countWorkOrders(@Param("containerId") Long containerId);

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM yard_sector
                WHERE sector_id = #{sectorId}
            )
            """)
    boolean existsYardSector(@Param("sectorId") Long sectorId);

    @Update("""
            UPDATE container
            SET can_exit = #{canExit}
            WHERE container_id = #{containerId}
            """)
    int updateCanExit(@Param("containerId") Long containerId, @Param("canExit") boolean canExit);
}
