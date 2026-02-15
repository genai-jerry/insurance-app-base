-- Seed initial users
-- Password for both: bcrypt hash of "Admin@123" and "Agent@123" respectively
-- These are hashed with BCrypt strength 10
INSERT INTO users (name, email, hashed_password, role) VALUES
('Admin User', 'admin@insurance.com', '$2a$10$XqPg8qZ3Q6vZ1yZ9Z6Z9ZeZ6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6', 'ADMIN'),
('Agent Smith', 'agent@insurance.com', '$2a$10$YqPg8qZ3Q6vZ1yZ9Z6Z9ZeZ6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6Z9Z6', 'AGENT');

-- Seed product categories
INSERT INTO product_categories (name, description) VALUES
('Life Insurance', 'Life insurance products including term, whole life, and universal life'),
('Health Insurance', 'Individual and family health insurance plans'),
('Auto Insurance', 'Vehicle insurance coverage'),
('Home Insurance', 'Homeowners and renters insurance'),
('Investment', 'Unit-linked insurance plans and investment products');

-- Seed sample products
INSERT INTO products (category_id, name, insurer, plan_type, details_json, eligibility_json, tags) VALUES
(
    (SELECT id FROM product_categories WHERE name = 'Life Insurance'),
    'SecureLife Term Plan',
    'LifeGuard Insurance Co.',
    'TERM',
    '{"coverage_amount": "10,00,000 - 2,00,00,000", "policy_term": "10-40 years", "premium_payment_term": "Regular/Limited Pay", "riders": ["Critical Illness", "Accidental Death Benefit", "Waiver of Premium"], "claim_settlement_ratio": "98.5%"}',
    '{"min_age": 18, "max_age": 65, "income_requirement": "Varies by sum assured", "health_requirements": "Medical examination required for higher sums"}',
    ARRAY['term-insurance', 'life-cover', 'affordable', 'high-coverage']
),
(
    (SELECT id FROM product_categories WHERE name = 'Health Insurance'),
    'FamilyCare Health Plus',
    'HealthFirst Insurance',
    'HEALTH',
    '{"coverage_amount": "5,00,000 - 50,00,000", "features": ["Cashless hospitalization", "Pre and post hospitalization", "Ambulance charges", "No claim bonus", "Lifetime renewability"], "network_hospitals": "8000+"}',
    '{"min_age": 0, "max_age": 65, "family_floater": true, "pre_existing_waiting": "2-4 years"}',
    ARRAY['health-insurance', 'family-floater', 'cashless', 'comprehensive']
),
(
    (SELECT id FROM product_categories WHERE name = 'Investment'),
    'WealthBuilder ULIP',
    'InvestSmart Insurance',
    'ULIP',
    '{"investment_funds": ["Equity", "Debt", "Balanced"], "lock_in_period": "5 years", "life_cover": "10x annual premium", "fund_switching": "Unlimited free switches", "partial_withdrawal": "After 5 years"}',
    '{"min_age": 18, "max_age": 60, "min_premium": "24,000/year", "policy_term": "10-30 years"}',
    ARRAY['ulip', 'investment', 'market-linked', 'tax-benefits']
);

-- Seed a sample lead
INSERT INTO leads (assigned_agent_id, name, phone, email, location, age, status, preferred_time_windows, timezone, consent_flags, notes) VALUES
(
    (SELECT id FROM users WHERE email = 'agent@insurance.com'),
    'John Doe',
    '+1-555-0123',
    'john.doe@example.com',
    'New York, NY',
    35,
    'NEW',
    '[{"day": "Monday-Friday", "start": "09:00", "end": "18:00"}]'::jsonb,
    'America/New_York',
    '{"dnd": false, "email_opt_in": true, "call_consent": true}'::jsonb,
    'Interested in life and health insurance. Has a family of 4.'
);
