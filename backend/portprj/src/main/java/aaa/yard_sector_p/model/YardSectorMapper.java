package aaa.yard_sector_p.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface YardSectorMapper {
    @Select("SELECT * FROM yard_sector ORDER BY sector_id")
    List<YardSectorDTO> list();

    @Select("""
            SELECT
                sector_id AS sectorId,
                sector_name AS sectorName,
                block_name AS blockName,
                sector_status AS sectorStatus,
                capacity,
                waiting_vehicle_count AS waitingVehicleCount,
                guide_message AS guideMessage,
                alt_waiting_area AS altWaitingArea
            FROM yard_sector
            WHERE sector_id = #{sectorId}
            """)
    YardSectorDTO detail(@Param("sectorId") Long sectorId);
}
