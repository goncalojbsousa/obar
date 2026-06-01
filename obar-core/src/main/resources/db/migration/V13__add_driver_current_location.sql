ALTER TABLE users
ADD COLUMN current_latitude FLOAT,
ADD COLUMN current_longitude FLOAT,
ADD COLUMN last_location_update TIMESTAMP;

UPDATE users
SET
    current_latitude = 41.6931991577,
    current_longitude = -8.8328695297,
    last_location_update = now ()
WHERE
    email = 'carlos.driver@gmail.com';

UPDATE users
SET
    current_latitude = 41.5454486,
    current_longitude = -8.426507,
    last_location_update = now ()
WHERE
    email = 'ricardo.driver@gmail.com';