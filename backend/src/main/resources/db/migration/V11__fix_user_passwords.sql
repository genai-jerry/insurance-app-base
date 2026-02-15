-- Fix user passwords with correct BCrypt hashes
-- BCrypt hashes generated with strength 10 for passwords "Admin@123" and "Agent@123"

UPDATE users
SET hashed_password = '$2b$10$MsSgYOvUmSY8CaB.2Q4XP.L0EGGSxx5/rmvh/ZUFnV1l4Q4gpYjC.'
WHERE email = 'admin@insurance.com';

UPDATE users
SET hashed_password = '$2b$10$7POS3s8NJna/V1FFs7znBerNGVXHF1YFi98NwpYetzup5YTy2nDJq'
WHERE email = 'agent@insurance.com';
