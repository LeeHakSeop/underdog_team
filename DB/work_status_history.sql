-- 작업 상태 변경 이력 테이블
-- 상태 코드는 work_order.work_status와 동일하게 사용합니다.
-- DISPATCH_WAITING, APPROVED, GATE_IN, IN_PROGRESS, COMPLETED, GATE_OUT, CANCELED
CREATE TABLE IF NOT EXISTS work_status_history(
    history_id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT REFERENCES work_order(work_order_id),
    prev_status VARCHAR(30),
    new_status VARCHAR(30),
    changed_time TIMESTAMP,
    changed_by VARCHAR(100),
    reason VARCHAR(255),
    remark VARCHAR(255)
);
