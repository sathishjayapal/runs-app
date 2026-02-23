-- Create test roles
-- Password:                                                                                                                                                                                                                   'password' encoded with {bcrypt} prefix

INSERT INTO runner_app_role (id, role_name, created_at, updated_at) VALUES
(10001, 'ROLE_ADMIN', NOW(), NOW()),
(10002, 'ROLE_USER', NOW(), NOW()),
(10003, 'ROLE_VIEWER', NOW(), NOW());

-- Create test users (password = 'password')
INSERT INTO run_app_user (id, email, password, name, created_at, updated_at) VALUES
(10004, 'admin@test.com', '{bcrypt}$2b$10$UtTTbqf1HeQPEwAJBnw.OOVBFpFc.gL//6SZlL05jY3h4hjOTQuYG', 'Admin User', NOW(), NOW()),
(10005, 'user@test.com', '{bcrypt}$2b$10$UtTTbqf1HeQPEwAJBnw.OOVBFpFc.gL//6SZlL05jY3h4hjOTQuYG', 'Regular User', NOW(), NOW()),
(10006, 'viewer@test.com', '{bcrypt}$2b$10$UtTTbqf1HeQPEwAJBnw.OOVBFpFc.gL//6SZlL05jY3h4hjOTQuYG', 'View Only', NOW(), NOW());

-- Assign roles to users
INSERT INTO run_app_user_roles (user_id, role_id) VALUES
(10004, 10001), -- Admin has ADMIN role
(10004, 10002), -- Admin also has USER role
(10005, 10002), -- Regular user has USER role
(10006, 10003); -- Viewer has VIEWER role
