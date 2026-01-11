ALTER TABLE runner_app_role DROP CONSTRAINT IF EXISTS fk_runner_app_role_runner_user_roles_id;

ALTER TABLE runner_app_role DROP COLUMN IF EXISTS runner_user_roles_id;

ALTER TABLE runner_app_role ADD COLUMN IF NOT EXISTS description VARCHAR(255);

CREATE TABLE IF NOT EXISTS run_app_user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT run_app_user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_run_app_user_roles_user_id FOREIGN KEY (user_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_run_app_user_roles_role_id FOREIGN KEY (role_id) REFERENCES runner_app_role (id) ON UPDATE NO ACTION ON DELETE CASCADE
);
