UPDATE vehicles
SET
    category = 'STANDARD'
WHERE
    upper(category) = 'ECO';

UPDATE trips
SET
    vehicle_category = 'STANDARD'
WHERE
    upper(vehicle_category) = 'ECO';

INSERT INTO
    vehicles (
        driver_id,
        brand,
        model,
        color,
        license_plate,
        year,
        category,
        base_fare,
        price_per_km,
        active
    )
SELECT
    d.id,
    'Mercedes-Benz',
    'Vito',
    'Black',
    'XL-2026-001',
    2026,
    'XL',
    5.00,
    1.15,
    true
FROM
    users d
WHERE
    d.email = 'carlos.driver@gmail.com'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            vehicles v
        WHERE
            v.license_plate = 'XL-2026-001'
    );

INSERT INTO
    vehicles (
        driver_id,
        brand,
        model,
        color,
        license_plate,
        year,
        category,
        base_fare,
        price_per_km,
        active
    )
SELECT
    d.id,
    'Mercedes-Benz',
    'E-Class',
    'White',
    'PR-2026-001',
    2026,
    'PREMIUM',
    7.50,
    1.60,
    true
FROM
    users d
WHERE
    d.email = 'ricardo.driver@gmail.com'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            vehicles v
        WHERE
            v.license_plate = 'PR-2026-001'
    );