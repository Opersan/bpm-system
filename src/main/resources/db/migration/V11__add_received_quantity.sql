-- Add received_quantity column to purchase_order_items
-- First add as nullable to avoid issues with existing data
ALTER TABLE purchase_order_items
    ADD COLUMN IF NOT EXISTS received_quantity INTEGER;

-- Update existing rows to have 0 as default
UPDATE purchase_order_items SET received_quantity = 0 WHERE received_quantity IS NULL;

-- Now add NOT NULL constraint
ALTER TABLE purchase_order_items
    ALTER COLUMN received_quantity SET NOT NULL;
