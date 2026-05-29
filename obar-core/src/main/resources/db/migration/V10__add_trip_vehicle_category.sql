ALTER TABLE trips
ADD COLUMN vehicle_category VARCHAR;

UPDATE trips t
SET
  vehicle_category = v.category
FROM
  vehicles v
WHERE
  t.vehicle_id = v.id
  AND t.vehicle_category IS NULL;

UPDATE trips
SET
  vehicle_category = 'STANDARD'
WHERE
  vehicle_category IS NULL;

ALTER TABLE trips
ALTER COLUMN vehicle_category
SET
  NOT NULL;