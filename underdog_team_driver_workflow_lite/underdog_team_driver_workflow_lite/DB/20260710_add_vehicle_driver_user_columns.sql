ALTER TABLE vehicle
ADD COLUMN IF NOT EXISTS driver_id BIGINT;

ALTER TABLE vehicle
ADD COLUMN IF NOT EXISTS user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vehicle_driver'
    ) THEN
        ALTER TABLE vehicle
        ADD CONSTRAINT fk_vehicle_driver
        FOREIGN KEY (driver_id)
        REFERENCES driver(driver_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_vehicle_user'
    ) THEN
        ALTER TABLE vehicle
        ADD CONSTRAINT fk_vehicle_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id);
    END IF;
END $$;
