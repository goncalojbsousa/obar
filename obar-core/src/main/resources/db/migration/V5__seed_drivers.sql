-- Seeds two driver users with vehicles
-- Carlos: Carlos123! -> $2a$12$DzNm7Q5fKbCTDBjL8lfO3Om47n8jTTYRg9X5.SOG5BPYUEsvWIFbO
-- Ricardo: Ricardo123! -> $2a$12$Mn/bMv6rRn730QN51m4ICuriwf.RmSjMBDEfLDBGKuu/Xf2IgFiEe

-- Insert driver users
INSERT INTO users (name, email, password_hash, phone, status, type, license_number, available, average_rating, total_trips)
VALUES
    (
        'Carlos',
        'carlos.driver@gmail.com',
        '$2a$12$DzNm7Q5fKbCTDBjL8lfO3Om47n8jTTYRg9X5.SOG5BPYUEsvWIFbO',
        '+351 910 123 456',
        'ACTIVE',
        'DRIVER',
        'PT123456789',
        true,
        4.8,
        47
    ),
    (
        'Ricardo',
        'ricardo.driver@gmail.com',
        '$2a$12$Mn/bMv6rRn730QN51m4ICuriwf.RmSjMBDEfLDBGKuu/Xf2IgFiEe',
        '+351 920 987 654',
        'ACTIVE',
        'DRIVER',
        'PT987654321',
        true,
        4.9,
        62
    )
ON CONFLICT (email) DO NOTHING;

-- Insert vehicles for drivers
INSERT INTO vehicles (driver_id, brand, model, color, license_plate, year, category, base_fare, price_per_km, active)
VALUES
    (
        (SELECT id FROM users WHERE email = 'carlos.driver@gmail.com' LIMIT 1),
        'Toyota',
        'Prius',
        'Silver',
        'CR-2024-001',
        2024,
        'ECO',
        5.50,
        0.85,
        true
    ),
    (
        (SELECT id FROM users WHERE email = 'ricardo.driver@gmail.com' LIMIT 1),
        'Volkswagen',
        'Polo',
        'Black',
        'RD-2024-002',
        2024,
        'STANDARD',
        6.00,
        0.95,
        true
    )
ON CONFLICT (license_plate) DO NOTHING;
