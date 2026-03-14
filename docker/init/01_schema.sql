-- ENUMS
CREATE TYPE "UserType" AS ENUM ('CLIENT', 'DRIVER', 'ADMIN');

CREATE TYPE "AccountStatus" AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING');

CREATE TYPE "TripStatus" AS ENUM (
    'PENDING',
    'ACCEPTED',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED',
    'REJECTED'
);

CREATE TYPE "TripDriverStatus" AS ENUM ('ASSIGNED', 'ACCEPTED', 'REJECTED', 'EXPIRED');

CREATE TYPE "TripType" AS ENUM ('IMMEDIATE', 'SCHEDULED');

CREATE TYPE "PaymentStatus" AS ENUM ('PENDING', 'PROCESSED', 'FAILED', 'REFUNDED');

CREATE TYPE "NotificationType" AS ENUM (
    'NEW_TRIP',
    'TRIP_ACCEPTED',
    'TRIP_CANCELLED',
    'PAYMENT'
);

-- TABELAS
CREATE TABLE
    "User" (
        "id" SERIAL PRIMARY KEY,
        "name" VARCHAR NOT NULL,
        "email" VARCHAR UNIQUE NOT NULL,
        "passwordHash" VARCHAR NOT NULL,
        "phone" VARCHAR,
        "status" "AccountStatus" NOT NULL DEFAULT 'ACTIVE',
        "createdAt" TIMESTAMP NOT NULL DEFAULT now (),
        "type" "UserType" NOT NULL,
        -- campos CLIENT
        "taxNumber" VARCHAR UNIQUE,
        "defaultPaymentMethodId" INT,
        -- campos DRIVER
        "licenseNumber" VARCHAR UNIQUE,
        "available" BOOLEAN DEFAULT false,
        "averageRating" FLOAT DEFAULT 0,
        "totalTrips" INT DEFAULT 0
    );

CREATE TABLE
    "Route" (
        "id" SERIAL PRIMARY KEY,
        "originAddress" VARCHAR NOT NULL,
        "destinationAddress" VARCHAR NOT NULL,
        "originLatitude" FLOAT,
        "originLongitude" FLOAT,
        "destinationLatitude" FLOAT,
        "destinationLongitude" FLOAT,
        "distanceKm" FLOAT,
        "estimatedDurationMin" INT
    );

CREATE TABLE
    "Vehicle" (
        "id" SERIAL PRIMARY KEY,
        "driverId" INT NOT NULL,
        "brand" VARCHAR NOT NULL,
        "model" VARCHAR NOT NULL,
        "color" VARCHAR,
        "licensePlate" VARCHAR UNIQUE NOT NULL,
        "year" INT,
        "category" VARCHAR NOT NULL,
        "baseFare" DECIMAL(10, 2),
        "pricePerKm" DECIMAL(10, 2),
        "active" BOOLEAN NOT NULL DEFAULT true
    );

CREATE TABLE
    "Trip" (
        "id" SERIAL PRIMARY KEY,
        "clientId" INT NOT NULL,
        "driverId" INT,
        "vehicleId" INT,
        "routeId" INT NOT NULL,
        "requestTime" TIMESTAMP NOT NULL DEFAULT now (),
        "startTime" TIMESTAMP,
        "endTime" TIMESTAMP,
        "status" "TripStatus" NOT NULL DEFAULT 'PENDING',
        "tripType" "TripType" NOT NULL,
        "cancelledBy" VARCHAR,
        "cancelReason" VARCHAR,
        "notes" VARCHAR,
        "estimatedPrice" DECIMAL(10, 2),
        "finalPrice" DECIMAL(10, 2)
    );

CREATE TABLE
    "TripDriver" (
        "id" SERIAL PRIMARY KEY,
        "tripId" INT NOT NULL,
        "driverId" INT NOT NULL,
        "status" "TripDriverStatus" NOT NULL DEFAULT 'ASSIGNED',
        "assignedAt" TIMESTAMP NOT NULL DEFAULT now (),
        "respondedAt" TIMESTAMP
    );

CREATE TABLE
    "PaymentMethod" (
        "id" SERIAL PRIMARY KEY,
        "clientId" INT NOT NULL,
        "type" VARCHAR NOT NULL,
        "details" VARCHAR,
        "active" BOOLEAN NOT NULL DEFAULT true
    );

CREATE TABLE
    "Payment" (
        "id" SERIAL PRIMARY KEY,
        "tripId" INT NOT NULL,
        "paymentMethodId" INT NOT NULL,
        "amount" DECIMAL(10, 2) NOT NULL,
        "paymentDate" TIMESTAMP NOT NULL DEFAULT now (),
        "status" "PaymentStatus" NOT NULL DEFAULT 'PENDING'
    );

CREATE TABLE
    "Review" (
        "id" SERIAL PRIMARY KEY,
        "tripId" INT NOT NULL,
        "reviewerId" INT NOT NULL,
        "reviewedId" INT NOT NULL,
        "rating" INT NOT NULL,
        "comment" VARCHAR,
        "createdAt" TIMESTAMP NOT NULL DEFAULT now (),
        "reviewerType" VARCHAR NOT NULL
    );

CREATE TABLE
    "Notification" (
        "id" SERIAL PRIMARY KEY,
        "userId" INT NOT NULL,
        "message" VARCHAR NOT NULL,
        "createdAt" TIMESTAMP NOT NULL DEFAULT now (),
        "read" BOOLEAN NOT NULL DEFAULT false,
        "type" "NotificationType" NOT NULL
    );

-- FOREIGN KEYS
ALTER TABLE "User" ADD FOREIGN KEY ("defaultPaymentMethodId") REFERENCES "PaymentMethod" ("id");

ALTER TABLE "Vehicle" ADD FOREIGN KEY ("driverId") REFERENCES "User" ("id");

ALTER TABLE "Trip" ADD FOREIGN KEY ("clientId") REFERENCES "User" ("id");

ALTER TABLE "Trip" ADD FOREIGN KEY ("driverId") REFERENCES "User" ("id");

ALTER TABLE "Trip" ADD FOREIGN KEY ("vehicleId") REFERENCES "Vehicle" ("id");

ALTER TABLE "Trip" ADD FOREIGN KEY ("routeId") REFERENCES "Route" ("id");

ALTER TABLE "TripDriver" ADD FOREIGN KEY ("tripId") REFERENCES "Trip" ("id");

ALTER TABLE "TripDriver" ADD FOREIGN KEY ("driverId") REFERENCES "User" ("id");

ALTER TABLE "PaymentMethod" ADD FOREIGN KEY ("clientId") REFERENCES "User" ("id");

ALTER TABLE "Payment" ADD FOREIGN KEY ("tripId") REFERENCES "Trip" ("id");

ALTER TABLE "Payment" ADD FOREIGN KEY ("paymentMethodId") REFERENCES "PaymentMethod" ("id");

ALTER TABLE "Review" ADD FOREIGN KEY ("tripId") REFERENCES "Trip" ("id");

ALTER TABLE "Review" ADD FOREIGN KEY ("reviewerId") REFERENCES "User" ("id");

ALTER TABLE "Review" ADD FOREIGN KEY ("reviewedId") REFERENCES "User" ("id");

ALTER TABLE "Notification" ADD FOREIGN KEY ("userId") REFERENCES "User" ("id");