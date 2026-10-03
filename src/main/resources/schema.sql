
-- =========================================================
-- BUS TICKETS DATABASE
-- =========================================================

CREATE DATABASE IF NOT EXISTS bus_tickets_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bus_tickets_db;


-- =========================================================
-- 1. ADMIN USERS
-- =========================================================

CREATE TABLE IF NOT EXISTS admin_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    email VARCHAR(20) UNIQUE NOT NULL,
    username VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(20) NOT NULL,
    admin_role ENUM('GLOBAL_ADMIN', 'LOCAL_ADMIN') NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) CHARACTER SET utf8mb4;


-- =========================================================
-- 2. BUS
-- =========================================================

CREATE TABLE IF NOT EXISTS bus (
    bus_id INT AUTO_INCREMENT PRIMARY KEY,
    bus_number VARCHAR(20) UNIQUE NOT NULL,
    seat_capacity INT NOT NULL,
    bus_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) CHARACTER SET utf8mb4;


-- =========================================================
-- 3. TRIP
-- =========================================================

CREATE TABLE IF NOT EXISTS trip (
    trip_id INT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    departure_location VARCHAR(20) NOT NULL,
    destination_location VARCHAR(20) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL
) CHARACTER SET utf8mb4;


-- =========================================================
-- 4. ROUTE
-- =========================================================

CREATE TABLE IF NOT EXISTS route (
    route_id INT AUTO_INCREMENT PRIMARY KEY,
    trip_id INT NOT NULL,
    route_date DATE NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    available_seats INT NOT NULL,
    ticket_price_kyat DECIMAL(12,2) NOT NULL,

    status ENUM(
        'SCHEDULED',
        'DEPARTED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'SCHEDULED',

    CONSTRAINT fk_route_trip
        FOREIGN KEY (trip_id)
        REFERENCES trip(trip_id)
) CHARACTER SET utf8mb4;


-- =========================================================
-- 5. BUS TRIP
-- =========================================================

CREATE TABLE IF NOT EXISTS bus_trip (
    bus_id INT NOT NULL,
    route_id INT NOT NULL,

    PRIMARY KEY (bus_id, route_id),

    FOREIGN KEY (bus_id)
        REFERENCES bus(bus_id),

    FOREIGN KEY (route_id)
        REFERENCES route(route_id)
) CHARACTER SET utf8mb4;


-- =========================================================
-- 6. TICKET
-- =========================================================

CREATE TABLE IF NOT EXISTS ticket (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    route_id INT NOT NULL,
    passenger_name VARCHAR(20) NOT NULL,
    passenger_phone VARCHAR(20) NOT NULL,
    ticket_date DATE NOT NULL,
    seat_number INT NOT NULL,
    number_of_seats INT NOT NULL DEFAULT 1,
    total_amount_kyat DECIMAL(12,2) NOT NULL,

    ticket_status ENUM(
        'BOOKED',
        'USED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'BOOKED',

    FOREIGN KEY (route_id)
        REFERENCES route(route_id),

    UNIQUE KEY uq_route_seat (
        route_id,
        seat_number,
        ticket_status
    )
) CHARACTER SET utf8mb4;


-- =========================================================
-- 7. DEFAULT ADMIN USERS
-- =========================================================

INSERT IGNORE INTO admin_users (
    name,
    email,
    username,
    password,
    admin_role
)
VALUES
(
    'အထွေထွေအုပ်ချုပ်သူ',
    'global@bus.local',
    'globaladmin',
    'global123',
    'GLOBAL_ADMIN'
),
(
    'ဒေသခံအုပ်ချုပ်သူ',
    'local@bus.local',
    'localadmin',
    'local123',
    'LOCAL_ADMIN'
);


-- =========================================================
-- 8. DEFAULT BUSES
-- =========================================================

INSERT IGNORE INTO bus (
    bus_number,
    seat_capacity,
    bus_type
)
VALUES
(
    'ရွှေမန္တလာ-001',
    45,
    'အမြန်ဘတ်စ်ကား'
),
(
    'မန်းမြန်-002',
    32,
    'VIP ဘတ်စ်ကား'
);


-- =========================================================
-- 9. DEFAULT TRIPS
-- =========================================================

INSERT IGNORE INTO trip (
    departure_location,
    destination_location,
    distance_km
)
VALUES
(
    'ရန်ကုန်',
    'မန္တလေး',
    620
),
(
    'ရန်ကုန်',
    'နေပြည်တော်',
    320
),
(
    'မန္တလေး',
    'ပုဂံ',
    180
);
USE bus_tickets_db;

SET SQL_SAFE_UPDATES = 0;

