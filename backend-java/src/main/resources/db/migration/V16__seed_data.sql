-- Seed data: Menu items for the platform
INSERT INTO menus (label, path) VALUES
('Dashboard',            '/dashboard'),
('ATOM Requests',        '/atom-requests'),
('CPSR Requests',        '/cpsr-requests'),
('Admin Users',          '/admin/users'),
('Admin Departments',    '/admin/departments'),
('Admin Roles',          '/admin/roles'),
('Admin Menus',          '/admin/menus'),
('Admin Codes',          '/admin/codes'),
('Activity Logs',        '/admin/activity-logs'),
('Toxicity DB',          '/toxicity-records'),
('LLM Inferences',       '/llm-inferences');

-- Seed data: Additional code groups
INSERT INTO code_groups (group_code, group_name, description) VALUES
('EVAL_STATUS',       'Evaluation Status',       'Status for CPSR toxicity evaluations'),
('PREDICTION_STATUS', 'Prediction Status',       'Status for ATOM AI predictions'),
('LLM_INFERENCE_TYPE','LLM Inference Type',      'Type of LLM inference request'),
('REGULATORY_FW',     'Regulatory Framework',     'Regulatory frameworks for CPSR'),
('TARGET_MARKET',     'Target Market',            'Target markets for products');

INSERT INTO codes (group_id, code_value, code_label, sort_order) VALUES
(5, 'PENDING',    'Pending',    1),
(5, 'IN_PROGRESS','In Progress',2),
(5, 'COMPLETED',  'Completed',  3),
(5, 'APPROVED',   'Approved',   4),
(5, 'REJECTED',   'Rejected',   5),
(5, 'CANCELLED',  'Cancelled',  6),
(6, 'INPUT_READY','Input Ready',1),
(6, 'QUEUED',     'Queued',     2),
(6, 'RUNNING',    'Running',    3),
(6, 'COMPLETED',  'Completed',  4),
(6, 'FAILED',     'Failed',     5),
(6, 'CANCELLED',  'Cancelled',  6),
(7, 'TOXICITY_ASSESSMENT','Toxicity Assessment', 1),
(7, 'CPSR_GENERATION',    'CPSR Generation',     2),
(7, 'SAFETY_REVIEW',      'Safety Review',       3),
(7, 'REFERENCE_LOOKUP',   'Reference Lookup',    4),
(8, 'EU_SCCS',    'EU SCCS',    1),
(8, 'K_REACH',    'K-REACH',    2),
(8, 'US_FDA',     'US FDA',     3),
(8, 'JP_NITE',    'JP NITE',    4),
(9, 'EU',         'EU',         1),
(9, 'KOREA',      'Korea',      2),
(9, 'GLOBAL',     'Global',     3),
(9, 'US',         'US',         4),
(9, 'JAPAN',      'Japan',      5);

-- Seed data: Default departments
INSERT INTO departments (name) VALUES
('R&D Center'),
('Quality Assurance'),
('Regulatory Affairs'),
('Production'),
('AI/ML Team');
