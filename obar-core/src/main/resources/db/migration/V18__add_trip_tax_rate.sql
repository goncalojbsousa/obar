ALTER TABLE trips
    ADD COLUMN tax_rate_applied DECIMAL(5, 4);

UPDATE
    trips t
SET
    tax_rate_applied = CASE WHEN TRIM(u.tax_number)
    LIKE '6%' THEN
        0.0000
    ELSE
        0.2300
    END
FROM
    users u
WHERE
    t.client_id = u.id
    AND t.tax_rate_applied IS NULL;

ALTER TABLE trips
    ALTER COLUMN tax_rate_applied SET NOT NULL;

