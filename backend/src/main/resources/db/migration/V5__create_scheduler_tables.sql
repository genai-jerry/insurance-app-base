-- Call tasks table for scheduling
CREATE TABLE call_tasks (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    agent_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'DONE', 'MISSED', 'CANCELLED')),
    outcome VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_call_tasks_lead ON call_tasks(lead_id);
CREATE INDEX idx_call_tasks_agent ON call_tasks(agent_id);
CREATE INDEX idx_call_tasks_scheduled_time ON call_tasks(scheduled_time);
CREATE INDEX idx_call_tasks_status ON call_tasks(status);
