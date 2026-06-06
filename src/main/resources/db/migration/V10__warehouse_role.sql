-- Add received_quantity column to purchase_order_items
ALTER TABLE purchase_order_items
    ADD COLUMN IF NOT EXISTS received_quantity INTEGER NOT NULL DEFAULT 0;

-- Add warehouse_user role to user_roles enum (if using enum)
-- If not using enum, create a new role entry
INSERT INTO roles (id, name, label, css_class) 
SELECT 7, 'WAREHOUSE_USER', 'Ambar Sorumlusu', 'neutral'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'WAREHOUSE_USER');

-- Add warehouse demo user
INSERT INTO users (id, username, full_name, email, password, role_id, active, created_at)
SELECT 
    COALESCE((SELECT MAX(id) + 1 FROM users), 1),
    'warehouse',
    'Ambar Sorumlusu',
    'warehouse@erp.com',
    '$2a$10$N9qo8uLOickgm2kzhZLsqdXPwyeG5n1Bk1J3BwvI7X5GJlL1J3Bw2', -- password123 hash
    COALESCE((SELECT id FROM roles WHERE name = 'WAREHOUSE_USER'), 7),
    true,
    NOW() - INTERVAL '50 days'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'warehouse');
