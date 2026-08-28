ALTER TABLE users ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
CREATE INDEX IF NOT EXISTS ix_users_department ON users(department_id);
