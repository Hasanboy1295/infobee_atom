ALTER TABLE cpsr_requests ADD COLUMN requester_name        VARCHAR(200);
ALTER TABLE cpsr_requests ADD COLUMN requester_email       VARCHAR(200);
ALTER TABLE cpsr_requests ADD COLUMN requester_phone       VARCHAR(50);
ALTER TABLE cpsr_requests ADD COLUMN company_name          VARCHAR(300);
ALTER TABLE cpsr_requests ADD COLUMN product_name          VARCHAR(300);
ALTER TABLE cpsr_requests ADD COLUMN regulatory_framework  VARCHAR(200);
ALTER TABLE cpsr_requests ADD COLUMN target_market         VARCHAR(200);
ALTER TABLE cpsr_requests ADD COLUMN additional_info       TEXT;
