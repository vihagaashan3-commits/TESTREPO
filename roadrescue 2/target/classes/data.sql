-- ================================================
-- RoadRescue Seed Data
-- Run after first application start
-- ================================================

-- Admin user (password: admin123)
INSERT INTO users (full_name, email, phone, password, role, is_active, is_deleted, created_at, updated_at)
VALUES ('Admin User', 'admin@roadrescue.com', '0771234567',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Hmm',
        'ROLE_ADMIN', true, false, NOW(), NOW());

-- Garage owner (password: owner123)
INSERT INTO users (full_name, email, phone, password, role, is_active, is_deleted, created_at, updated_at)
VALUES ('Kamal Perera', 'kamal@garage.com', '0779876543',
        '$2a$10$slYQmyNdgTY18LXQqSDzUORlcTQMBTtOirTTbGnMp1YOtGH6SjVaq',
        'ROLE_GARAGE_OWNER', true, false, NOW(), NOW());

-- Regular user (password: user123)
INSERT INTO users (full_name, email, phone, password, role, is_active, is_deleted, created_at, updated_at)
VALUES ('Nimal Silva', 'nimal@gmail.com', '0765551234',
        '$2a$10$qI4u8/4QhVSiBl1KYFqy.OB9mAF8tqHvFG36oYVBajK9MCp7dU',
        'ROLE_USER', true, false, NOW(), NOW());

-- Garages
INSERT INTO garages (garage_name, address, phone, email, latitude, longitude,
                     opening_time, closing_time, is_available, is_deleted, is_verified,
                     owner_id, created_at, updated_at)
VALUES
('City Auto Repair', '45 Galle Road, Colombo 3', '0112345678', 'cityauto@gmail.com',
 6.9000, 79.8600, '08:00', '18:00', true, false, true, 2, NOW(), NOW()),
('Perera Motors', '12 Kandy Road, Kelaniya', '0112987654', 'pereramotors@gmail.com',
 7.0000, 79.9200, '07:30', '19:00', true, false, true, 2, NOW(), NOW()),
('QuickFix Garage', '78 High Level Road, Nugegoda', '0114567890', 'quickfix@gmail.com',
 6.8700, 79.8900, '08:00', '20:00', true, false, true, 2, NOW(), NOW());

-- Garage services
INSERT INTO garage_services (garage_id, service_type) VALUES
(1, 'FLAT_TIRE'), (1, 'BATTERY_JUMP'), (1, 'ENGINE_REPAIR'), (1, 'GENERAL_REPAIR'),
(2, 'TOWING'), (2, 'FUEL_DELIVERY'), (2, 'FLAT_TIRE'), (2, 'BRAKE_REPAIR'),
(3, 'BATTERY_JUMP'), (3, 'BATTERY_REPLACEMENT'), (3, 'OIL_CHANGE'), (3, 'LOCKOUT');

-- Technicians
INSERT INTO technicians (name, phone, experience_years, is_available, is_deleted, garage_id, created_at, updated_at)
VALUES
('Sunil Fernando', '0771112222', 5, true, false, 1, NOW(), NOW()),
('Ravi Kumar', '0772223333', 8, true, false, 1, NOW(), NOW()),
('Ajith Bandara', '0773334444', 3, true, false, 2, NOW(), NOW()),
('Prasad Jayawardena', '0774445555', 10, true, false, 3, NOW(), NOW());

-- Technician skills
INSERT INTO technician_skills (technician_id, skill) VALUES
(1, 'FLAT_TIRE'), (1, 'BATTERY_JUMP'),
(2, 'ENGINE_REPAIR'), (2, 'GENERAL_REPAIR'),
(3, 'TOWING'), (3, 'FUEL_DELIVERY'),
(4, 'BATTERY_REPLACEMENT'), (4, 'OIL_CHANGE');

-- Sample vehicles
INSERT INTO vehicles (vehicle_type, brand, model, plate_number, year, color, is_deleted, user_id, created_at, updated_at)
VALUES
('Car', 'Toyota', 'Corolla', 'CAB-1234', 2018, 'White', false, 3, NOW(), NOW()),
('Car', 'Honda', 'Civic', 'WP-5678', 2020, 'Blue', false, 3, NOW(), NOW());
