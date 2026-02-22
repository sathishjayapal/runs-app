-- Create test roles
-- Password hash: $2a$10$ZLhnHxdpHETcxmtEStgpI.JQTLXsXdh4R9qZpCIzqNdM9JhvB5EjO = 'password'

INSERT INTO runner_app_role (id, role_name, created_at, updated_at) VALUES
(10001, 'ADMIN', NOW(), NOW()),
(10002, 'USER', NOW(), NOW()),
(10003, 'VIEWER', NOW(), NOW());

-- Create test users
INSERT INTO run_app_user (id, email, password, name, created_at, updated_at) VALUES
(10004, 'admin@test.com', '$2a$10$ZLhnHxdpHETcxmtEStgpI.JQTLXsXdh4R9qZpCIzqNdM9JhvB5EjO', 'Admin User', NOW(), NOW()),
(10005, 'user@test.com', '$2a$10$ZLhnHxdpHETcxmtEStgpI.JQTLXsXdh4R9qZpCIzqNdM9JhvB5EjO', 'Regular User', NOW(), NOW()),
(10006, 'viewer@test.com', '$2a$10$ZLhnHxdpHETcxmtEStgpI.JQTLXsXdh4R9qZpCIzqNdM9JhvB5EjO', 'View Only', NOW(), NOW());

-- Assign roles to users
INSERT INTO run_app_user_roles (user_id, role_id) VALUES
(10004, 10001), -- Admin has ADMIN role
(10004, 10002), -- Admin also has USER role
(10005, 10002), -- Regular user has USER role
(10006, 10003); -- Viewer has VIEWER role
