-- ENUMS
CREATE TYPE user_type AS ENUM ('CLIENT', 'DRIVER', 'ADMIN');

CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING');

CREATE TYPE trip_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED',
    'REJECTED'
);

CREATE TYPE trip_driver_status AS ENUM ('ASSIGNED', 'ACCEPTED', 'REJECTED', 'EXPIRED');

CREATE TYPE trip_type AS ENUM ('IMMEDIATE', 'SCHEDULED');

CREATE TYPE payment_status AS ENUM ('PENDING', 'PROCESSED', 'FAILED', 'REFUNDED');

CREATE TYPE notification_type AS ENUM (
    'NEW_TRIP',
    'TRIP_ACCEPTED',
    'TRIP_CANCELLED',
    'PAYMENT'
);

-- TABLES
CREATE TABLE
    users (
        id SERIAL PRIMARY KEY,
        name VARCHAR NOT NULL,
        email VARCHAR UNIQUE NOT NULL,
        password_hash VARCHAR NOT NULL,
        phone VARCHAR,
        status account_status NOT NULL DEFAULT 'ACTIVE',
        created_at TIMESTAMP NOT NULL DEFAULT now (),
        type user_type NOT NULL,
        -- CLIENT FIELDS
        tax_number VARCHAR UNIQUE,
        default_payment_method_id INT,
        -- DRIVER FIELDS
        license_number VARCHAR UNIQUE,
        available BOOLEAN DEFAULT false,
        average_rating FLOAT DEFAULT 0,
        total_trips INT DEFAULT 0
    );

CREATE TABLE
    routes (
        id SERIAL PRIMARY KEY,
        origin_address VARCHAR NOT NULL,
        destination_address VARCHAR NOT NULL,
        origin_latitude FLOAT,
        origin_longitude FLOAT,
        destination_latitude FLOAT,
        destination_longitude FLOAT,
        distance_km FLOAT,
        estimated_duration_min INT
    );

CREATE TABLE
    vehicles (
        id SERIAL PRIMARY KEY,
        driver_id INT NOT NULL,
        brand VARCHAR NOT NULL,
        model VARCHAR NOT NULL,
        color VARCHAR,
        license_plate VARCHAR UNIQUE NOT NULL,
        year INT,
        category VARCHAR NOT NULL,
        base_fare DECIMAL(10, 2),
        price_per_km DECIMAL(10, 2),
        active BOOLEAN NOT NULL DEFAULT true
    );

CREATE TABLE
    trips (
        id SERIAL PRIMARY KEY,
        client_id INT NOT NULL,
        driver_id INT,
        vehicle_id INT,
        route_id INT NOT NULL,
        request_time TIMESTAMP NOT NULL DEFAULT now (),
        start_time TIMESTAMP,
        end_time TIMESTAMP,
        status trip_status NOT NULL DEFAULT 'PENDING',
        trip_type trip_type NOT NULL,
        cancelled_by VARCHAR,
        cancel_reason VARCHAR,
        notes VARCHAR,
        estimated_price DECIMAL(10, 2),
        final_price DECIMAL(10, 2)
    );

CREATE TABLE
    trip_drivers (
        id SERIAL PRIMARY KEY,
        trip_id INT NOT NULL,
        driver_id INT NOT NULL,
        status trip_driver_status NOT NULL DEFAULT 'ASSIGNED',
        assigned_at TIMESTAMP NOT NULL DEFAULT now (),
        responded_at TIMESTAMP
    );

CREATE TABLE
    payment_methods (
        id SERIAL PRIMARY KEY,
        client_id INT NOT NULL,
        type VARCHAR NOT NULL,
        details VARCHAR,
        active BOOLEAN NOT NULL DEFAULT true
    );

CREATE TABLE
    payments (
        id SERIAL PRIMARY KEY,
        trip_id INT NOT NULL,
        payment_method_id INT NOT NULL,
        amount DECIMAL(10, 2) NOT NULL,
        payment_date TIMESTAMP NOT NULL DEFAULT now (),
        status payment_status NOT NULL DEFAULT 'PENDING'
    );

CREATE TABLE
    reviews (
        id SERIAL PRIMARY KEY,
        trip_id INT NOT NULL,
        reviewer_id INT NOT NULL,
        reviewed_id INT NOT NULL,
        rating INT NOT NULL,
        comment VARCHAR,
        created_at TIMESTAMP NOT NULL DEFAULT now (),
        reviewer_type VARCHAR NOT NULL
    );

CREATE TABLE
    notifications (
        id SERIAL PRIMARY KEY,
        user_id INT NOT NULL,
        message VARCHAR NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT now (),
        read BOOLEAN NOT NULL DEFAULT false,
        type notification_type NOT NULL
    );

-- FOREIGN KEYS
ALTER TABLE users ADD FOREIGN KEY (default_payment_method_id) REFERENCES payment_methods (id);

ALTER TABLE vehicles ADD FOREIGN KEY (driver_id) REFERENCES users (id);

ALTER TABLE trips ADD FOREIGN KEY (client_id) REFERENCES users (id);

ALTER TABLE trips ADD FOREIGN KEY (driver_id) REFERENCES users (id);

ALTER TABLE trips ADD FOREIGN KEY (vehicle_id) REFERENCES vehicles (id);

ALTER TABLE trips ADD FOREIGN KEY (route_id) REFERENCES routes (id);

ALTER TABLE trip_drivers ADD FOREIGN KEY (trip_id) REFERENCES trips (id);

ALTER TABLE trip_drivers ADD FOREIGN KEY (driver_id) REFERENCES users (id);

ALTER TABLE payment_methods ADD FOREIGN KEY (client_id) REFERENCES users (id);

ALTER TABLE payments ADD FOREIGN KEY (trip_id) REFERENCES trips (id);

ALTER TABLE payments ADD FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id);

ALTER TABLE reviews ADD FOREIGN KEY (trip_id) REFERENCES trips (id);

ALTER TABLE reviews ADD FOREIGN KEY (reviewer_id) REFERENCES users (id);

ALTER TABLE reviews ADD FOREIGN KEY (reviewed_id) REFERENCES users (id);

ALTER TABLE notifications ADD FOREIGN KEY (user_id) REFERENCES users (id);