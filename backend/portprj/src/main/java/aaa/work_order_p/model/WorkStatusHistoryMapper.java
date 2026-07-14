package aaa.work_order_p.model;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface WorkStatusHistoryMapper {

    @Insert("""
            INSERT INTO work_status_history (
                work_order_id,
                prev_status,
                new_status,
                changed_time,
                changed_by,
                reason,
                remark
            ) VALUES (
                #{workOrderId},
                #{prevStatus},
                #{newStatus},
                #{changedTime},
                #{changedBy},
                #{reason},
                #{remark}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "historyId", keyColumn = "history_id")
    int insert(WorkStatusHistoryDTO history);
}
