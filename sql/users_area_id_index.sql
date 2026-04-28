-- Acelera JOIN/WHERE por área en colaboradores (User.area_id → areas).
CREATE INDEX IF NOT EXISTS idx_users_area_id ON users (area_id);
