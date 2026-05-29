ALTER TABLE trips
ADD COLUMN scheduled_time TIMESTAMP;

UPDATE trips
SET
  scheduled_time = start_time
WHERE
  trip_type = 'SCHEDULED'
  AND scheduled_time IS NULL;