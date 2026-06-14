WITH ranked_active_vehicles AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY driver_id
            ORDER BY id
        ) AS active_rank
    FROM vehicles
    WHERE active = true
      AND removed = false
)
UPDATE vehicles
SET active = false
WHERE id IN (
    SELECT id
    FROM ranked_active_vehicles
    WHERE active_rank > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicles_one_active_per_driver
    ON vehicles (driver_id)
    WHERE active = true
      AND removed = false;
