-- ==================== ROLES ====================
-- Kreiranje rola
INSERT INTO role (id, name) VALUES (1, 'ROLE_USER');
INSERT INTO role (id, name) VALUES (2, 'ROLE_ADMIN');

-- ==================== TEST USERS ====================
-- Lozinke su heširane pomoću BCrypt algoritma
-- Svi test korisnici imaju lozinku: "password123"
-- BCrypt hash: $2a$10$ebyC4Z5WqHCXKp.6YqbmweNJQGqMT.aqDDdJQqIqVqQqWP8r5G5A6

-- Test korisnik 1 - AKTIVIRAN (email: user@test.com, lozinka: password123)
INSERT INTO users (id, email, username, password, first_name, last_name, address, activated, activation_token, enabled, last_password_reset_date)
VALUES (1, 'user@test.com', 'testuser', '$2a$10$ebyC4Z5WqHCXKp.6YqbmweNJQGqMT.aqDDdJQqIqVqQqWP8r5G5A6', 'Marko', 'Marković', 'Bulevar oslobođenja 46, Novi Sad', true, null, true, '2024-01-01 12:00:00');

-- Test korisnik 2 - NEAKTIVIRAN (email: newuser@test.com, lozinka: password123)
INSERT INTO users (id, email, username, password, first_name, last_name, address, activated, activation_token, enabled, last_password_reset_date)
VALUES (2, 'newuser@test.com', 'newuser', '$2a$10$ebyC4Z5WqHCXKp.6YqbmweNJQGqMT.aqDDdJQqIqVqQqWP8r5G5A6', 'Nikola', 'Nikolić', 'Kralja Petra 10, Beograd', false, 'test-activation-token-123', true, '2024-01-01 12:00:00');

-- Test admin - AKTIVIRAN (email: admin@test.com, lozinka: password123)
INSERT INTO users (id, email, username, password, first_name, last_name, address, activated, activation_token, enabled, last_password_reset_date)
VALUES (3, 'admin@test.com', 'admin', '$2a$10$ebyC4Z5WqHCXKp.6YqbmweNJQGqMT.aqDDdJQqIqVqQqWP8r5G5A6', 'Ana', 'Anić', 'Zmaj Jovina 5, Sombor', true, null, true, '2024-01-01 12:00:00');

-- ==================== USER ROLES ====================
-- Dodela rola korisnicima

-- testuser -> ROLE_USER
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);

-- newuser -> ROLE_USER
INSERT INTO user_role (user_id, role_id) VALUES (2, 1);

-- admin -> ROLE_USER i ROLE_ADMIN
INSERT INTO user_role (user_id, role_id) VALUES (3, 1);
INSERT INTO user_role (user_id, role_id) VALUES (3, 2);