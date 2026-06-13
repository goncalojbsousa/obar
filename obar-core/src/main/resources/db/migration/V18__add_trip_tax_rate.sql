ALTER TABLE trips
    ADD COLUMN tax_rate_applied DECIMAL(5, 4);

UPDATE
    trips
SET
    tax_rate_applied = 0.2300
WHERE
    tax_rate_applied IS NULL;

ALTER TABLE trips
    ALTER COLUMN tax_rate_applied SET NOT NULL;

