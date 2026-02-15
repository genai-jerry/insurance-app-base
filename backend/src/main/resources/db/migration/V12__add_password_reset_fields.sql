-- Add password reset token fields to users table

ALTER TABLE users
ADD COLUMN reset_token VARCHAR(255),
ADD COLUMN reset_token_expiry TIMESTAMP;

-- Add index on reset_token for faster lookups
CREATE INDEX idx_users_reset_token ON users(reset_token);
