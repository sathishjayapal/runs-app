-- Create role table
CREATE TABLE role (
                      id BIGINT NOT NULL,
                      name VARCHAR(50) NOT NULL UNIQUE,
                      created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                      updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                      CONSTRAINT role_pkey PRIMARY KEY (id)
);

-- Create role sequence
CREATE SEQUENCE role_sequence START WITH 1 INCREMENT BY 1;

-- Insert default roles
INSERT INTO role (id, name, created_at, updated_at)
VALUES
    (1, 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Add role_id column to users table
ALTER TABLE users ADD COLUMN role_id BIGINT;

-- Migrate existing data: set all existing users to USER role
UPDATE users SET role_id = 2 WHERE role_id IS NULL;

-- Make role_id NOT NULL after setting defaults
ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;

-- Add foreign key constraint
ALTER TABLE users ADD CONSTRAINT fk_users_role_id FOREIGN KEY (role_id) REFERENCES role(id);

-- Drop old role VARCHAR column
ALTER TABLE users DROP COLUMN role;
