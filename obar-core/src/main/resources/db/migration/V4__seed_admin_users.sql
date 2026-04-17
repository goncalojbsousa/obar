-- Seeds the initial admin users
-- Hashes generated with bcrypt (12 rounds, $2a$ for jbcrypt 0.4 compatibility)
-- Goncalo123! -> $2a$12$q33/5npGg37tAksjEks0xeZYSvuhJN228qi68pUTXqEcAW4Bo2hGC
-- Miguel123!  -> $2a$12$oYzt4fQyrGBmqCqUPVFo/.rKfCCnhkwNVWgmnsBce1BEUz5lAB4bq
INSERT INTO users (name, email, password_hash, status, type)
VALUES
    (
        'goncalo',
        'goncalo@gmail.com',
        '$2a$12$q33/5npGg37tAksjEks0xeZYSvuhJN228qi68pUTXqEcAW4Bo2hGC',
        'ACTIVE',
        'ADMIN'
    ),
    (
        'miguel',
        'miguel@gmail.com',
        '$2a$12$oYzt4fQyrGBmqCqUPVFo/.rKfCCnhkwNVWgmnsBce1BEUz5lAB4bq',
        'ACTIVE',
        'ADMIN'
    )
ON CONFLICT (email) DO NOTHING;
