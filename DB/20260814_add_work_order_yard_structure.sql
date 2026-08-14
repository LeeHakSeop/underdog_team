BEGIN;

ALTER TABLE work_order
    ADD COLUMN IF NOT EXISTS start_sector_id BIGINT;

ALTER TABLE work_order
    ADD COLUMN IF NOT EXISTS destination_sector_id BIGINT;

ALTER TABLE yard_sector
    ADD COLUMN IF NOT EXISTS environment_type VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_work_order_start_sector'
          AND conrelid = 'work_order'::regclass
    ) THEN
        ALTER TABLE work_order
            ADD CONSTRAINT fk_work_order_start_sector
            FOREIGN KEY (start_sector_id) REFERENCES yard_sector(sector_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_work_order_destination_sector'
          AND conrelid = 'work_order'::regclass
    ) THEN
        ALTER TABLE work_order
            ADD CONSTRAINT fk_work_order_destination_sector
            FOREIGN KEY (destination_sector_id) REFERENCES yard_sector(sector_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_yard_sector_environment_type'
          AND conrelid = 'yard_sector'::regclass
    ) THEN
        ALTER TABLE yard_sector
            ADD CONSTRAINT ck_yard_sector_environment_type
            CHECK (
                environment_type IS NULL
                OR environment_type IN ('GENERAL', 'HEAVY', 'REEFER', 'DANGEROUS', 'EMPTY')
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_work_order_start_sector_id
    ON work_order(start_sector_id);

CREATE INDEX IF NOT EXISTS idx_work_order_destination_sector_id
    ON work_order(destination_sector_id);

-- For active legacy work, the container's current sector is a reliable start point.
UPDATE work_order wo
SET start_sector_id = c.sector_id
FROM container c
WHERE c.container_id = wo.container_id
  AND c.sector_id IS NOT NULL
  AND wo.start_sector_id IS NULL
  AND wo.work_status IN ('DISPATCH_WAITING', 'APPROVED', 'GATE_IN', 'IN_PROGRESS');

-- Only descriptions with an unambiguous environment are backfilled.
UPDATE yard_sector
SET environment_type = CASE sector_name
    WHEN 'A-01' THEN 'GENERAL'
    WHEN 'B-01' THEN 'GENERAL'
    WHEN 'C-01' THEN 'REEFER'
END
WHERE environment_type IS NULL
  AND sector_name IN ('A-01', 'B-01', 'C-01');

COMMIT;

-- Manual rollback after dependent application data has been reviewed:
-- ALTER TABLE work_order DROP CONSTRAINT IF EXISTS fk_work_order_destination_sector;
-- ALTER TABLE work_order DROP CONSTRAINT IF EXISTS fk_work_order_start_sector;
-- DROP INDEX IF EXISTS idx_work_order_destination_sector_id;
-- DROP INDEX IF EXISTS idx_work_order_start_sector_id;
-- ALTER TABLE work_order DROP COLUMN IF EXISTS destination_sector_id;
-- ALTER TABLE work_order DROP COLUMN IF EXISTS start_sector_id;
-- ALTER TABLE yard_sector DROP CONSTRAINT IF EXISTS ck_yard_sector_environment_type;
-- ALTER TABLE yard_sector DROP COLUMN IF EXISTS environment_type;
