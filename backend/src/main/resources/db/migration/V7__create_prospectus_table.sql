-- Prospectus table
CREATE TABLE prospectus (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    agent_id BIGINT NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    voice_session_id BIGINT REFERENCES voice_sessions(id) ON DELETE SET NULL,
    version INTEGER NOT NULL DEFAULT 1,
    html_content TEXT NOT NULL,
    pdf_path VARCHAR(500),
    pdf_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prospectus_lead ON prospectus(lead_id);
CREATE INDEX idx_prospectus_agent ON prospectus(agent_id);
CREATE INDEX idx_prospectus_voice_session ON prospectus(voice_session_id);
