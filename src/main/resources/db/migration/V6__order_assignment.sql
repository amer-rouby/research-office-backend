ALTER TABLE service_order ADD COLUMN assigned_expert_id UUID REFERENCES app_user (id);
CREATE INDEX service_order_expert_idx ON service_order (assigned_expert_id);
