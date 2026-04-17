-- Seeds two client users
-- Custodio: Custodio123! -> $2a$12$AXMiJcNRKeYUMzjd.cqb9eTxJZasUszuFqu2upN0.SzPV68PE01tW
-- Humberto: Humberto123! -> $2a$12$mVov3K4btiWb7zXsVQ/LqOitybzEnjnmEhXb9DbdZDZK1M1NHrHbC

INSERT INTO users (
    name,
    email,
    password_hash,
    phone,
    status,
    type,
    tax_number,
    tax_category,
    default_tax_rate_id,
    default_currency_id
)
VALUES
    (
        'Custodio',
        'custodio@gmail.com',
        '$2a$12$AXMiJcNRKeYUMzjd.cqb9eTxJZasUszuFqu2upN0.SzPV68PE01tW',
        '+351 930 111 222',
        'ACTIVE',
        'CLIENT',
        '123456780',
        'INDIVIDUAL',
        (SELECT id FROM tax_rates WHERE name = 'Standard IVA' LIMIT 1),
        (SELECT id FROM currencies WHERE code = 'EUR' LIMIT 1)
    ),
    (
        'Humberto',
        'humberto@gmail.com',
        '$2a$12$mVov3K4btiWb7zXsVQ/LqOitybzEnjnmEhXb9DbdZDZK1M1NHrHbC',
        '+351 930 333 444',
        'ACTIVE',
        'CLIENT',
        '123456781',
        'INDIVIDUAL',
        (SELECT id FROM tax_rates WHERE name = 'Standard IVA' LIMIT 1),
        (SELECT id FROM currencies WHERE code = 'EUR' LIMIT 1)
    )
ON CONFLICT (email) DO NOTHING;
