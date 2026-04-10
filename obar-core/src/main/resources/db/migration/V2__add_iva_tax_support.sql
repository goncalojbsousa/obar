-- V2__add_iva_tax_support.sql
-- Adds IVA support
-- ENUM: client fiscal/tax category
CREATE TYPE client_tax_category AS ENUM (
    'INDIVIDUAL', -- Normal person
    'COMPANY', -- Company with NIF
    'EXEMPT_ENTITY' -- IPSS, public entities
);

-- TABLE: tax_rates
CREATE TABLE
    tax_rates (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        rate DECIMAL(5, 4) NOT NULL,
        description VARCHAR(255),
        active BOOLEAN NOT NULL DEFAULT true
    );

-- Seed the standard Portuguese IVA rates (mainland)
INSERT INTO
    tax_rates (name, rate, description)
VALUES
    (
        'Standard IVA',
        0.2300,
        'Standard IVA rate: individuals and companies (23%)'
    ),
    (
        'Reduced IVA',
        0.0600,
        'Reduced IVA rate: applicable to certain services (6%)'
    ),
    (
        'IVA Exempt',
        0.0000,
        'IVA exempt: exempt entities such as IPSS or public bodies'
    );

-- ALTER TABLE: users
ALTER TABLE users
ADD COLUMN tax_category client_tax_category, -- set for CLIENT users
ADD COLUMN default_tax_rate_id INT REFERENCES tax_rates (id);

-- ALTER TABLE: payments
-- Record the exact tax applied at billing time because the client may bill to a different NIF at checkout.
ALTER TABLE payments
ADD COLUMN tax_rate_id INT REFERENCES tax_rates (id), -- which rate was used
ADD COLUMN tax_rate_applied DECIMAL(5, 4), -- snapshot of rate
ADD COLUMN net_amount DECIMAL(10, 2), -- amount before IVA
ADD COLUMN tax_amount DECIMAL(10, 2), -- IVA portion
ADD COLUMN billing_nif VARCHAR, -- NIF used at billing (may differ from client default)
ADD COLUMN billing_name VARCHAR;

-- Name on the invoice/receipt