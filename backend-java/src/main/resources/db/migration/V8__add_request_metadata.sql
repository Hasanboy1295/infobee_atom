ALTER TABLE atom_requests ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
ALTER TABLE atom_requests ADD COLUMN IF NOT EXISTS priority VARCHAR(16);
ALTER TABLE atom_requests ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE atom_requests ADD COLUMN IF NOT EXISTS tags TEXT;

ALTER TABLE cpsr_requests ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id);
ALTER TABLE cpsr_requests ADD COLUMN IF NOT EXISTS priority VARCHAR(16);
ALTER TABLE cpsr_requests ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE cpsr_requests ADD COLUMN IF NOT EXISTS tags TEXT;

CREATE INDEX IF NOT EXISTS ix_atom_requests_department ON atom_requests(department_id);
CREATE INDEX IF NOT EXISTS ix_cpsr_requests_department ON cpsr_requests(department_id);
CREATE INDEX IF NOT EXISTS ix_atom_requests_priority ON atom_requests(priority);
CREATE INDEX IF NOT EXISTS ix_cpsr_requests_priority ON cpsr_requests(priority);
