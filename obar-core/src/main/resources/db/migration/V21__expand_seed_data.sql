-- Expands the demo dataset to 30 clients, 12 drivers, 15 vehicles and
-- 900 additional trips spread across roughly 18 months.
-- All generated users use the seed password Carlos123!.

-- Keep the original accounts but give them stable Random User photos.
UPDATE users
SET
    photo_url = CASE email
        WHEN 'custodio@gmail.com' THEN 'https://randomuser.me/api/portraits/men/99.jpg'
        WHEN 'humberto@gmail.com' THEN 'https://randomuser.me/api/portraits/men/96.jpg'
        WHEN 'carlos.driver@gmail.com' THEN 'https://randomuser.me/api/portraits/men/41.jpg'
        WHEN 'ricardo.driver@gmail.com' THEN 'https://randomuser.me/api/portraits/men/4.jpg'
        ELSE photo_url
    END,
    online = CASE
        WHEN email = 'ricardo.driver@gmail.com' THEN true
        ELSE false
    END,
    available = CASE
        WHEN type = 'DRIVER' THEN false
        ELSE available
    END
WHERE email IN (
    'custodio@gmail.com',
    'humberto@gmail.com',
    'carlos.driver@gmail.com',
    'ricardo.driver@gmail.com'
);

-- Add 28 clients. Email, tax number and photo URL are stable natural keys.
WITH client_seed (
    name,
    email,
    phone,
    tax_number,
    photo_url,
    created_days_ago
) AS (
    VALUES
        ('Lillian Fox', 'lillian.fox@seed.obar.pt', '+351 931 000 001', '245000001', 'https://randomuser.me/api/portraits/women/87.jpg', 510),
        ('Maria Black', 'maria.black@seed.obar.pt', '+351 931 000 002', '245000002', 'https://randomuser.me/api/portraits/women/68.jpg', 492),
        ('Alyssia Robert', 'alyssia.robert@seed.obar.pt', '+351 931 000 003', '245000003', 'https://randomuser.me/api/portraits/women/45.jpg', 474),
        ('Ian Simmons', 'ian.simmons@seed.obar.pt', '+351 931 000 004', '245000004', 'https://randomuser.me/api/portraits/men/10.jpg', 456),
        ('Line Rolland', 'line.rolland@seed.obar.pt', '+351 931 000 005', '245000005', 'https://randomuser.me/api/portraits/women/78.jpg', 438),
        ('Amy Kuhn', 'amy.kuhn@seed.obar.pt', '+351 931 000 006', '245000006', 'https://randomuser.me/api/portraits/women/17.jpg', 420),
        ('Leandre Perez', 'leandre.perez@seed.obar.pt', '+351 931 000 007', '245000007', 'https://randomuser.me/api/portraits/men/43.jpg', 402),
        ('Patricia Sanz', 'patricia.sanz@seed.obar.pt', '+351 931 000 008', '245000008', 'https://randomuser.me/api/portraits/women/88.jpg', 384),
        ('Veronica Herrero', 'veronica.herrero@seed.obar.pt', '+351 931 000 009', '245000009', 'https://randomuser.me/api/portraits/women/12.jpg', 366),
        ('Gerardo Castillo', 'gerardo.castillo@seed.obar.pt', '+351 931 000 010', '245000010', 'https://randomuser.me/api/portraits/men/44.jpg', 348),
        ('Lidia Blanco', 'lidia.blanco@seed.obar.pt', '+351 931 000 011', '245000011', 'https://randomuser.me/api/portraits/women/84.jpg', 330),
        ('Alicia Ruiz', 'alicia.ruiz@seed.obar.pt', '+351 931 000 012', '245000012', 'https://randomuser.me/api/portraits/women/51.jpg', 312),
        ('Reginald Kuhn', 'reginald.kuhn@seed.obar.pt', '+351 931 000 013', '245000013', 'https://randomuser.me/api/portraits/men/86.jpg', 294),
        ('Glen Fowler', 'glen.fowler@seed.obar.pt', '+351 931 000 014', '245000014', 'https://randomuser.me/api/portraits/men/53.jpg', 276),
        ('Maelyne Durand', 'maelyne.durand@seed.obar.pt', '+351 931 000 015', '245000015', 'https://randomuser.me/api/portraits/women/34.jpg', 258),
        ('Marin Masson', 'marin.masson@seed.obar.pt', '+351 931 000 016', '245000016', 'https://randomuser.me/api/portraits/men/90.jpg', 240),
        ('Teresa Rivera', 'teresa.rivera@seed.obar.pt', '+351 931 000 017', '245000017', 'https://randomuser.me/api/portraits/women/31.jpg', 222),
        ('Todd Matthews', 'todd.matthews@seed.obar.pt', '+351 931 000 018', '245000018', 'https://randomuser.me/api/portraits/men/0.jpg', 204),
        ('Maiwenn Gerard', 'maiwenn.gerard@seed.obar.pt', '+351 931 000 019', '245000019', 'https://randomuser.me/api/portraits/women/92.jpg', 186),
        ('Samantha Brewer', 'samantha.brewer@seed.obar.pt', '+351 931 000 020', '245000020', 'https://randomuser.me/api/portraits/women/49.jpg', 168),
        ('Felix Herrero', 'felix.herrero@seed.obar.pt', '+351 931 000 021', '245000021', 'https://randomuser.me/api/portraits/men/55.jpg', 150),
        ('Leslie West', 'leslie.west@seed.obar.pt', '+351 931 000 022', '245000022', 'https://randomuser.me/api/portraits/men/18.jpg', 132),
        ('Andrew Larson', 'andrew.larson@seed.obar.pt', '+351 931 000 023', '245000023', 'https://randomuser.me/api/portraits/men/80.jpg', 114),
        ('Felix Sanchez', 'felix.sanchez@seed.obar.pt', '+351 931 000 024', '245000024', 'https://randomuser.me/api/portraits/men/38.jpg', 96),
        ('Tracy Martin', 'tracy.martin@seed.obar.pt', '+351 931 000 025', '245000025', 'https://randomuser.me/api/portraits/men/6.jpg', 78),
        ('Elea Girard', 'elea.girard@seed.obar.pt', '+351 931 000 026', '245000026', 'https://randomuser.me/api/portraits/women/18.jpg', 60),
        ('Henry Evans', 'henry.evans@seed.obar.pt', '+351 931 000 027', '245000027', 'https://randomuser.me/api/portraits/men/81.jpg', 42),
        ('Heidi Carpenter', 'heidi.carpenter@seed.obar.pt', '+351 931 000 028', '245000028', 'https://randomuser.me/api/portraits/women/85.jpg', 24)
)
INSERT INTO users (
    name,
    email,
    password_hash,
    phone,
    photo_url,
    status,
    created_at,
    type,
    tax_number,
    tax_category,
    default_tax_rate_id,
    default_currency_id,
    available,
    online
)
SELECT
    cs.name,
    cs.email,
    '$2a$12$DzNm7Q5fKbCTDBjL8lfO3Om47n8jTTYRg9X5.SOG5BPYUEsvWIFbO',
    cs.phone,
    cs.photo_url,
    'ACTIVE',
    now() - make_interval(days => cs.created_days_ago),
    'CLIENT',
    cs.tax_number,
    'INDIVIDUAL',
    (SELECT id FROM tax_rates WHERE name = 'Standard IVA' ORDER BY id LIMIT 1),
    (SELECT id FROM currencies WHERE code = 'EUR' ORDER BY id LIMIT 1),
    false,
    false
FROM client_seed cs
ON CONFLICT (email) DO UPDATE
SET photo_url = EXCLUDED.photo_url;

-- Add 10 active drivers around Braga, Guimaraes, Barcelos and Viana.
WITH driver_seed (
    name,
    email,
    phone,
    license_number,
    photo_url,
    latitude,
    longitude,
    rating,
    available,
    online
) AS (
    VALUES
        ('Maelys Bonnet', 'maelys.bonnet.driver@seed.obar.pt', '+351 932 100 001', 'PTSEED001', 'https://randomuser.me/api/portraits/women/33.jpg', 41.5454, -8.4265, 4.7, false, false),
        ('Greg Weaver', 'greg.weaver.driver@seed.obar.pt', '+351 932 100 002', 'PTSEED002', 'https://randomuser.me/api/portraits/men/97.jpg', 41.5518, -8.4229, 4.8, false, false),
        ('Elizabeth Rhodes', 'elizabeth.rhodes.driver@seed.obar.pt', '+351 932 100 003', 'PTSEED003', 'https://randomuser.me/api/portraits/women/59.jpg', 41.4444, -8.2962, 4.9, false, false),
        ('Ernest Patterson', 'ernest.patterson.driver@seed.obar.pt', '+351 932 100 004', 'PTSEED004', 'https://randomuser.me/api/portraits/men/72.jpg', 41.5388, -8.6151, 4.6, false, false),
        ('Chloe Grant', 'chloe.grant.driver@seed.obar.pt', '+351 932 100 005', 'PTSEED005', 'https://randomuser.me/api/portraits/women/77.jpg', 41.6932, -8.8329, 4.9, false, true),
        ('Nolan Gauthier', 'nolan.gauthier.driver@seed.obar.pt', '+351 932 100 006', 'PTSEED006', 'https://randomuser.me/api/portraits/men/46.jpg', 41.7671, -8.5839, 4.8, false, false),
        ('Gonzalo Alvarez', 'gonzalo.alvarez.driver@seed.obar.pt', '+351 932 100 007', 'PTSEED007', 'https://randomuser.me/api/portraits/men/35.jpg', 41.4050, -8.5222, 4.7, false, true),
        ('Emy Olivier', 'emy.olivier.driver@seed.obar.pt', '+351 932 100 008', 'PTSEED008', 'https://randomuser.me/api/portraits/women/80.jpg', 41.3530, -8.7430, 4.9, false, true),
        ('Elmer Hayes', 'elmer.hayes.driver@seed.obar.pt', '+351 932 100 009', 'PTSEED009', 'https://randomuser.me/api/portraits/men/9.jpg', 41.6918, -8.4280, 4.6, false, false),
        ('Barbara Graves', 'barbara.graves.driver@seed.obar.pt', '+351 932 100 010', 'PTSEED010', 'https://randomuser.me/api/portraits/women/38.jpg', 41.5780, -8.2700, 4.8, false, false)
)
INSERT INTO users (
    name,
    email,
    password_hash,
    phone,
    photo_url,
    status,
    created_at,
    type,
    license_number,
    available,
    online,
    average_rating,
    total_trips,
    current_latitude,
    current_longitude,
    last_location_update
)
SELECT
    ds.name,
    ds.email,
    '$2a$12$DzNm7Q5fKbCTDBjL8lfO3Om47n8jTTYRg9X5.SOG5BPYUEsvWIFbO',
    ds.phone,
    ds.photo_url,
    'ACTIVE',
    now() - interval '18 months' + make_interval(days => row_number() OVER (ORDER BY ds.email)::int * 21),
    'DRIVER',
    ds.license_number,
    ds.available,
    ds.online,
    ds.rating,
    0,
    ds.latitude,
    ds.longitude,
    now()
FROM driver_seed ds
ON CONFLICT (email) DO UPDATE
SET photo_url = EXCLUDED.photo_url;

-- Match the four existing vehicles to the first four permanent car assets.
UPDATE vehicles
SET
    brand = CASE license_plate
        WHEN 'CR-2024-001' THEN 'BMW'
        WHEN 'RD-2024-002' THEN 'BMW'
        WHEN 'XL-2026-001' THEN 'Citroen'
        WHEN 'PR-2026-001' THEN 'Ford'
        ELSE brand
    END,
    model = CASE license_plate
        WHEN 'CR-2024-001' THEN '116d'
        WHEN 'RD-2024-002' THEN '225xe Active Tourer'
        WHEN 'XL-2026-001' THEN 'C5 X Hybrid Shine'
        WHEN 'PR-2026-001' THEN 'Focus SW EcoBoost'
        ELSE model
    END,
    photo_url = CASE license_plate
        WHEN 'CR-2024-001' THEN 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/bmw-116d.jpg'
        WHEN 'RD-2024-002' THEN 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/bmw-225xe-active-tourer.jpg'
        WHEN 'XL-2026-001' THEN 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/citroen-c5x-hybrid.jpg'
        WHEN 'PR-2026-001' THEN 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/ford-focus-sw.jpg'
        ELSE photo_url
    END
WHERE license_plate IN ('CR-2024-001', 'RD-2024-002', 'XL-2026-001', 'PR-2026-001');

-- Add 11 vehicles so every permanent car photo is represented once.
WITH vehicle_seed (
    driver_email,
    brand,
    model,
    color,
    license_plate,
    year,
    category,
    base_fare,
    price_per_km,
    photo_url
) AS (
    VALUES
        ('maelys.bonnet.driver@seed.obar.pt', 'Mercedes-Benz', 'A 160 CDI Style', 'White', 'SE-01-AA', 2021, 'STANDARD', 4.50, 0.82, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/mercedes-a160.jpg'),
        ('greg.weaver.driver@seed.obar.pt', 'Mercedes-Benz', 'CLA 250', 'Black', 'SE-02-AB', 2022, 'PREMIUM', 7.50, 1.55, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/mercedes-cla250.jpg'),
        ('elizabeth.rhodes.driver@seed.obar.pt', 'MINI', 'One Sport Edition', 'Blue', 'SE-03-AC', 2020, 'STANDARD', 4.20, 0.80, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/mini-one-sport.jpg'),
        ('ernest.patterson.driver@seed.obar.pt', 'MINI', 'Countryman Cooper SE', 'Green', 'SE-04-AD', 2023, 'XL', 5.80, 1.12, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/mini-countryman-se.jpg'),
        ('chloe.grant.driver@seed.obar.pt', 'Nissan', 'Qashqai N-Connecta', 'Grey', 'SE-05-AE', 2022, 'XL', 5.50, 1.08, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/nissan-qashqai.jpg'),
        ('nolan.gauthier.driver@seed.obar.pt', 'Peugeot', '208 PureTech', 'Yellow', 'SE-06-AF', 2021, 'STANDARD', 4.10, 0.79, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/peugeot-208.jpg'),
        ('gonzalo.alvarez.driver@seed.obar.pt', 'Peugeot', '3008 Hybrid Allure', 'White', 'SE-07-AG', 2023, 'XL', 5.90, 1.15, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/peugeot-3008-hybrid.jpg'),
        ('emy.olivier.driver@seed.obar.pt', 'Peugeot', '308 BlueHDi', 'Red', 'SE-08-AH', 2022, 'STANDARD', 4.60, 0.86, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/peugeot-308.jpg'),
        ('elmer.hayes.driver@seed.obar.pt', 'Peugeot', '308 SW BlueHDi', 'Blue', 'SE-09-AJ', 2021, 'XL', 5.20, 1.02, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/peugeot-308-sw.jpg'),
        ('barbara.graves.driver@seed.obar.pt', 'Peugeot', '508 SW', 'Black', 'SE-10-AK', 2023, 'PREMIUM', 7.20, 1.48, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/peugeot-508-sw.jpg'),
        ('barbara.graves.driver@seed.obar.pt', 'Tesla', 'Model 3', 'White', 'SE-11-AL', 2024, 'PREMIUM', 7.80, 1.60, 'https://oqgdapuibldohxfnbkfo.supabase.co/storage/v1/object/public/vehicle-photos/seed/cars/tesla-model-3.jpg')
)
INSERT INTO vehicles (
    driver_id,
    brand,
    model,
    color,
    license_plate,
    year,
    category,
    base_fare,
    price_per_km,
    active,
    photo_url
)
SELECT
    u.id,
    vs.brand,
    vs.model,
    vs.color,
    vs.license_plate,
    vs.year,
    vs.category,
    vs.base_fare,
    vs.price_per_km,
    true,
    vs.photo_url
FROM vehicle_seed vs
JOIN users u ON u.email = vs.driver_email
ON CONFLICT (license_plate) DO UPDATE
SET photo_url = EXCLUDED.photo_url;

-- Add realistic routes around northern Portugal.
WITH route_seed (
    origin_address,
    destination_address,
    origin_latitude,
    origin_longitude,
    destination_latitude,
    destination_longitude,
    distance_km,
    estimated_duration_min
) AS (
    VALUES
        ('Braga Centro', 'Universidade do Minho', 41.5503, -8.4201, 41.5607, -8.3975, 4.2, 12),
        ('Braga', 'Guimaraes', 41.5454, -8.4265, 41.4444, -8.2962, 25.3, 28),
        ('Braga', 'Barcelos', 41.5454, -8.4265, 41.5388, -8.6151, 23.6, 27),
        ('Braga', 'Vila Verde', 41.5454, -8.4265, 41.6477, -8.4379, 14.8, 20),
        ('Braga', 'Famalicao', 41.5454, -8.4265, 41.4050, -8.5222, 24.1, 29),
        ('Braga', 'Porto Aeroporto', 41.5454, -8.4265, 41.2421, -8.6786, 54.0, 43),
        ('Guimaraes Centro', 'Braga', 41.4444, -8.2962, 41.5454, -8.4265, 25.1, 29),
        ('Guimaraes', 'Fafe', 41.4444, -8.2962, 41.4542, -8.1680, 14.1, 20),
        ('Guimaraes', 'Vizela', 41.4444, -8.2962, 41.3821, -8.3089, 10.6, 17),
        ('Barcelos Centro', 'Esposende', 41.5388, -8.6151, 41.5362, -8.7820, 17.9, 23),
        ('Barcelos', 'Viana do Castelo', 41.5388, -8.6151, 41.6932, -8.8329, 37.8, 38),
        ('Viana do Castelo', 'Ponte de Lima', 41.6932, -8.8329, 41.7671, -8.5839, 30.5, 31),
        ('Viana do Castelo', 'Caminha', 41.6932, -8.8329, 41.8764, -8.8383, 28.7, 29),
        ('Ponte de Lima', 'Arcos de Valdevez', 41.7671, -8.5839, 41.8467, -8.4191, 23.4, 27),
        ('Ponte de Lima', 'Ponte da Barca', 41.7671, -8.5839, 41.8065, -8.4197, 19.0, 23),
        ('Famalicao', 'Santo Tirso', 41.4050, -8.5222, 41.3431, -8.4738, 12.8, 18),
        ('Famalicao', 'Trofa', 41.4050, -8.5222, 41.3374, -8.5596, 11.2, 16),
        ('Vila do Conde', 'Povoa de Varzim', 41.3530, -8.7430, 41.3834, -8.7636, 5.8, 11),
        ('Povoa de Varzim', 'Porto Aeroporto', 41.3834, -8.7636, 41.2421, -8.6786, 27.4, 25),
        ('Braga Estacao', 'Bom Jesus do Monte', 41.5487, -8.4340, 41.5546, -8.3773, 7.9, 17),
        ('Guimaraes Estacao', 'Castelo de Guimaraes', 41.4354, -8.2941, 41.4479, -8.2907, 3.1, 9),
        ('Viana Estacao', 'Praia do Cabedelo', 41.6945, -8.8312, 41.6806, -8.8236, 5.4, 13),
        ('Braga', 'Gerês', 41.5454, -8.4265, 41.7280, -8.1620, 42.0, 47),
        ('Barcelos', 'Braga', 41.5388, -8.6151, 41.5454, -8.4265, 23.5, 26)
)
INSERT INTO routes (
    origin_address,
    destination_address,
    origin_latitude,
    origin_longitude,
    destination_latitude,
    destination_longitude,
    distance_km,
    estimated_duration_min
)
SELECT rs.*
FROM route_seed rs
WHERE NOT EXISTS (
    SELECT 1
    FROM routes r
    WHERE lower(r.origin_address) = lower(rs.origin_address)
      AND lower(r.destination_address) = lower(rs.destination_address)
);

-- Give every client two payment methods. Existing methods remain untouched.
INSERT INTO payment_methods (client_id, type, details, active)
SELECT
    u.id,
    method.type,
    CASE method.type
        WHEN 'MB Way' THEN replace(coalesce(u.phone, '+351930000000'), ' ', '')
        ELSE 'VISA **** ' || lpad((1000 + u.id % 9000)::text, 4, '0')
    END,
    true
FROM users u
CROSS JOIN (VALUES ('MB Way'), ('Cartao de Credito')) method(type)
WHERE u.type = 'CLIENT'
  AND NOT EXISTS (
      SELECT 1
      FROM payment_methods pm
      WHERE pm.client_id = u.id
        AND lower(pm.type) = lower(method.type)
  );

UPDATE users u
SET default_payment_method_id = pm.id
FROM payment_methods pm
WHERE u.type = 'CLIENT'
  AND u.default_payment_method_id IS NULL
  AND pm.client_id = u.id
  AND lower(pm.type) = 'mb way';

-- Generate 900 deterministic trip rows. The newest rows represent current
-- pending/accepted/in-progress activity; older rows are historical.
WITH
seed_rows AS (
    SELECT generate_series(1, 900) AS n
),
clients AS (
    SELECT id, row_number() OVER (ORDER BY email) AS rn
    FROM users
    WHERE type = 'CLIENT'
),
drivers AS (
    SELECT id, row_number() OVER (ORDER BY email) AS rn
    FROM users
    WHERE type = 'DRIVER' AND status = 'ACTIVE'
),
vehicle_pool AS (
    SELECT id, driver_id, category, row_number() OVER (ORDER BY license_plate) AS rn
    FROM vehicles
    WHERE active = true
),
route_pool AS (
    SELECT id, distance_km, estimated_duration_min, row_number() OVER (ORDER BY id) AS rn
    FROM routes
),
counts AS (
    SELECT
        (SELECT count(*) FROM clients) AS client_count,
        (SELECT count(*) FROM vehicle_pool) AS vehicle_count,
        (SELECT count(*) FROM route_pool) AS route_count
),
trip_data AS (
    SELECT
        sr.n,
        c.id AS client_id,
        vp.driver_id,
        vp.id AS vehicle_id,
        vp.category AS vehicle_category,
        rp.id AS route_id,
        rp.distance_km,
        rp.estimated_duration_min,
        CASE
            WHEN sr.n > 890
                THEN now() - make_interval(mins => (900 - sr.n) * 30)
            ELSE now() - make_interval(hours => (900 - sr.n) * 14)
                - make_interval(mins => (sr.n % 8) * 7)
        END AS requested_at,
        CASE
            WHEN sr.n = 891 OR sr.n = 892 OR sr.n = 899 OR sr.n = 900 THEN 'PENDING'
            WHEN sr.n BETWEEN 893 AND 895 THEN 'ACCEPTED'
            WHEN sr.n BETWEEN 896 AND 898 THEN 'COMPLETED'
            WHEN sr.n % 20 = 0 THEN 'CANCELLED'
            WHEN sr.n % 20 = 1 THEN 'REJECTED'
            ELSE 'COMPLETED'
        END::trip_status AS trip_status,
        CASE
            WHEN sr.n >= 899 OR (sr.n <= 890 AND sr.n % 7 = 0) THEN 'SCHEDULED'
            ELSE 'IMMEDIATE'
        END::trip_type AS trip_type,
        round((6.00 + coalesce(rp.distance_km, 10) * (0.72 + (sr.n % 5) * 0.06))::numeric, 2) AS estimated_price
    FROM seed_rows sr
    CROSS JOIN counts ct
    JOIN clients c ON c.rn = ((sr.n - 1) % ct.client_count) + 1
    JOIN vehicle_pool vp ON vp.rn = ((sr.n * 7 - 1) % ct.vehicle_count) + 1
    JOIN route_pool rp ON rp.rn = ((sr.n * 11 - 1) % ct.route_count) + 1
)
INSERT INTO trips (
    client_id,
    driver_id,
    vehicle_id,
    vehicle_category,
    route_id,
    request_time,
    scheduled_time,
    start_time,
    end_time,
    status,
    trip_type,
    cancelled_by,
    cancel_reason,
    notes,
    estimated_price,
    final_price,
    tax_rate_applied,
    start_pin
)
SELECT
    td.client_id,
    CASE WHEN td.trip_status IN ('PENDING', 'REJECTED') THEN NULL ELSE td.driver_id END,
    CASE WHEN td.trip_status IN ('PENDING', 'REJECTED') THEN NULL ELSE td.vehicle_id END,
    td.vehicle_category,
    td.route_id,
    td.requested_at,
    CASE
        WHEN td.trip_type = 'SCHEDULED' THEN td.requested_at + interval '18 hours'
        ELSE NULL
    END,
    CASE
        WHEN td.trip_status IN ('COMPLETED', 'IN_PROGRESS')
            THEN td.requested_at
                + CASE WHEN td.trip_type = 'SCHEDULED' THEN interval '18 hours' ELSE interval '6 minutes' END
        ELSE NULL
    END,
    CASE
        WHEN td.trip_status = 'COMPLETED'
            THEN td.requested_at
                + CASE WHEN td.trip_type = 'SCHEDULED' THEN interval '18 hours' ELSE interval '6 minutes' END
                + make_interval(mins => greatest(coalesce(td.estimated_duration_min, 15), 8))
        ELSE NULL
    END,
    td.trip_status,
    td.trip_type,
    CASE
        WHEN td.trip_status = 'CANCELLED' AND td.n % 2 = 0 THEN 'CLIENT'
        WHEN td.trip_status = 'CANCELLED' THEN 'DRIVER'
        ELSE NULL
    END,
    CASE
        WHEN td.trip_status = 'CANCELLED' AND td.n % 2 = 0 THEN 'Alteracao de planos'
        WHEN td.trip_status = 'CANCELLED' THEN 'Indisponibilidade inesperada'
        ELSE NULL
    END,
    'seed_bulk_trip_' || lpad(td.n::text, 4, '0'),
    td.estimated_price,
    CASE
        WHEN td.trip_status = 'COMPLETED'
            THEN round((td.estimated_price * (0.96 + (td.n % 9) * 0.01))::numeric, 2)
        ELSE NULL
    END,
    0.2300,
    CASE
        WHEN td.trip_status IN ('ACCEPTED', 'IN_PROGRESS')
            THEN lpad((1000 + td.n % 9000)::text, 4, '0')
        ELSE NULL
    END
FROM trip_data td
WHERE NOT EXISTS (
    SELECT 1
    FROM trips t
    WHERE t.notes = 'seed_bulk_trip_' || lpad(td.n::text, 4, '0')
);

-- Carlos is reserved for the presentation flow and must not start with an
-- accepted or in-progress trip. Reassign any generated active trip before
-- creating dispatch history.
UPDATE trips trip
SET
    driver_id = replacement.driver_id,
    vehicle_id = replacement.vehicle_id,
    vehicle_category = replacement.category
FROM users carlos
CROSS JOIN LATERAL (
    SELECT
        vehicle.driver_id,
        vehicle.id AS vehicle_id,
        vehicle.category
    FROM vehicles vehicle
    JOIN users driver ON driver.id = vehicle.driver_id
    WHERE driver.email NOT IN (
        'carlos.driver@gmail.com',
        'ricardo.driver@gmail.com'
    )
      AND vehicle.active = true
    ORDER BY vehicle.license_plate
    LIMIT 1
) replacement
WHERE trip.driver_id = carlos.id
  AND carlos.email = 'carlos.driver@gmail.com'
  AND trip.status IN ('ACCEPTED', 'IN_PROGRESS');

-- Keep active demo assignments exclusive: Ricardo remains only on the
-- historical in-progress trip, while recent accepted immediate trips are
-- assigned to three distinct drivers.
WITH active_assignment (trip_note, driver_email) AS (
    VALUES
        ('seed_bulk_trip_0893', 'emy.olivier.driver@seed.obar.pt'),
        ('seed_bulk_trip_0894', 'gonzalo.alvarez.driver@seed.obar.pt'),
        ('seed_bulk_trip_0895', 'chloe.grant.driver@seed.obar.pt')
)
UPDATE trips trip
SET
    driver_id = driver.id,
    vehicle_id = vehicle.id,
    vehicle_category = vehicle.category
FROM active_assignment assignment
JOIN users driver ON driver.email = assignment.driver_email
JOIN LATERAL (
    SELECT candidate.id, candidate.category
    FROM vehicles candidate
    WHERE candidate.driver_id = driver.id
      AND candidate.active = true
    ORDER BY candidate.license_plate
    LIMIT 1
) vehicle ON true
WHERE trip.notes = assignment.trip_note;

-- Record the dispatch/acceptance history for trips that reached a driver.
INSERT INTO trip_drivers (
    trip_id,
    driver_id,
    status,
    assigned_at,
    responded_at
)
SELECT
    t.id,
    t.driver_id,
    'ACCEPTED',
    t.request_time + interval '1 minute',
    t.request_time + interval '2 minutes'
FROM trips t
WHERE t.notes LIKE 'seed_bulk_trip_%'
  AND t.driver_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM trip_drivers td WHERE td.trip_id = t.id
  );

-- Add payments for all completed generated trips.
WITH standard_tax AS (
    SELECT id, rate
    FROM tax_rates
    WHERE name = 'Standard IVA'
    ORDER BY id
    LIMIT 1
),
eur AS (
    SELECT id FROM currencies WHERE code = 'EUR' ORDER BY id LIMIT 1
),
completed AS (
    SELECT
        t.*,
        substring(t.notes FROM '([0-9]+)$')::int AS seed_number
    FROM trips t
    WHERE t.notes LIKE 'seed_bulk_trip_%'
      AND t.status = 'COMPLETED'
)
INSERT INTO payments (
    trip_id,
    payment_method_id,
    amount,
    payment_date,
    status,
    tax_rate_id,
    tax_rate_applied,
    net_amount,
    tax_amount,
    billing_nif,
    billing_name,
    currency_id,
    exchange_rate_to_eur
)
SELECT
    t.id,
    pm.id,
    t.final_price,
    t.end_time + interval '5 minutes',
    CASE
        WHEN t.seed_number % 47 = 0 THEN 'REFUNDED'
        WHEN t.seed_number % 31 = 0 THEN 'FAILED'
        ELSE 'PROCESSED'
    END::payment_status,
    st.id,
    st.rate,
    round((t.final_price / (1 + st.rate))::numeric, 2),
    round((t.final_price - t.final_price / (1 + st.rate))::numeric, 2),
    client.tax_number,
    client.name,
    eur.id,
    1.000000
FROM completed t
JOIN users client ON client.id = t.client_id
JOIN LATERAL (
    SELECT payment_method.id
    FROM payment_methods payment_method
    WHERE payment_method.client_id = t.client_id
      AND payment_method.active = true
    ORDER BY
        CASE
            WHEN t.seed_number % 2 = 0 AND lower(payment_method.type) = 'mb way' THEN 0
            WHEN t.seed_number % 2 = 1 AND lower(payment_method.type) = 'cartao de credito' THEN 0
            ELSE 1
        END,
        payment_method.id
    LIMIT 1
) pm ON true
CROSS JOIN standard_tax st
CROSS JOIN eur
WHERE NOT EXISTS (
    SELECT 1 FROM payments p WHERE p.trip_id = t.id
);

-- Most completed trips receive a client review; half also receive a driver review.
WITH reviewable AS (
    SELECT
        t.*,
        substring(t.notes FROM '([0-9]+)$')::int AS seed_number
    FROM trips t
    WHERE t.notes LIKE 'seed_bulk_trip_%'
      AND t.status = 'COMPLETED'
      AND substring(t.notes FROM '([0-9]+)$')::int % 3 <> 0
)
INSERT INTO reviews (
    trip_id,
    reviewer_id,
    reviewed_id,
    rating,
    comment,
    created_at,
    reviewer_type
)
SELECT
    t.id,
    t.client_id,
    t.driver_id,
    CASE WHEN t.seed_number % 11 = 0 THEN 3 WHEN t.seed_number % 4 = 0 THEN 4 ELSE 5 END,
    CASE t.seed_number % 5
        WHEN 0 THEN 'Viagem tranquila e motorista muito profissional.'
        WHEN 1 THEN 'Chegou a horas e o carro estava impecavel.'
        WHEN 2 THEN 'Servico rapido e confortavel.'
        WHEN 3 THEN 'Boa comunicacao durante toda a viagem.'
        ELSE 'Experiencia muito positiva.'
    END,
    t.end_time + interval '20 minutes',
    'CLIENT'
FROM reviewable t
ON CONFLICT (trip_id, reviewer_type) DO NOTHING;

WITH reviewable AS (
    SELECT
        t.*,
        substring(t.notes FROM '([0-9]+)$')::int AS seed_number
    FROM trips t
    WHERE t.notes LIKE 'seed_bulk_trip_%'
      AND t.status = 'COMPLETED'
      AND substring(t.notes FROM '([0-9]+)$')::int % 2 = 0
)
INSERT INTO reviews (
    trip_id,
    reviewer_id,
    reviewed_id,
    rating,
    comment,
    created_at,
    reviewer_type
)
SELECT
    t.id,
    t.driver_id,
    t.client_id,
    CASE WHEN t.seed_number % 13 = 0 THEN 4 ELSE 5 END,
    CASE WHEN t.seed_number % 4 = 0
        THEN 'Cliente pontual e cordial.'
        ELSE 'Tudo correu bem durante a viagem.'
    END,
    t.end_time + interval '18 minutes',
    'DRIVER'
FROM reviewable t
ON CONFLICT (trip_id, reviewer_type) DO NOTHING;

-- Make driver dashboard aggregates reflect the generated history.
UPDATE users driver
SET
    total_trips = stats.completed_trips,
    average_rating = coalesce(stats.average_rating, driver.average_rating)
FROM (
    SELECT
        d.id AS driver_id,
        count(DISTINCT t.id) FILTER (WHERE t.status = 'COMPLETED')::int AS completed_trips,
        round(avg(r.rating)::numeric, 2)::float AS average_rating
    FROM users d
    LEFT JOIN trips t ON t.driver_id = d.id
    LEFT JOIN reviews r
        ON r.trip_id = t.id
       AND r.reviewed_id = d.id
       AND r.reviewer_type = 'CLIENT'
    WHERE d.type = 'DRIVER'
    GROUP BY d.id
) stats
WHERE driver.id = stats.driver_id;
