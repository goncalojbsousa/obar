-- V3__add_currency_support.sql
-- Adds multi-currency support
-- TABLE: currencies
CREATE TABLE
    currencies (
        id SERIAL PRIMARY KEY,
        code VARCHAR(3) NOT NULL UNIQUE,
        name VARCHAR(100) NOT NULL,
        symbol VARCHAR(10) NOT NULL,
        active BOOLEAN NOT NULL DEFAULT true
    );

-- Seed common currencies
INSERT INTO
    currencies (code, name, symbol)
VALUES
    ('EUR', 'Euro', '€'),
    ('USD', 'US Dollar', '$'),
    ('GBP', 'British Pound', '£'),
    ('BRL', 'Brazilian Real', 'R$'),
    ('CHF', 'Swiss Franc', 'Fr');

-- ALTER TABLE: users
-- Add default currency for CLIENT user
ALTER TABLE users
ADD COLUMN default_currency_id INT REFERENCES currencies (id);

-- ALTER TABLE: payments
-- Record the currency and exchange rate at billing time.
ALTER TABLE payments
ADD COLUMN currency_id INT REFERENCES currencies (id), -- currency used at checkout
ADD COLUMN exchange_rate_to_eur DECIMAL(18, 6);

-- rate at payment time