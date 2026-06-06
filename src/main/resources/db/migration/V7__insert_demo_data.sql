-- Insert Suppliers
INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT NOW(), NOW(), 'TechComponents Ltd', 'sales@techcomponents.com', '123 Tech Park, Silicon Valley'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'TechComponents Ltd');

INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT NOW(), NOW(), 'MetalWorks Inc', 'orders@metalworks.com', '456 Industrial Ave, Detroit'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'MetalWorks Inc');

INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT NOW(), NOW(), 'Global Logistics', 'contact@globallogistics.com', '789 Harbor Rd, Seattle'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'Global Logistics');

-- Insert Warehouses
INSERT INTO warehouses (created_at, updated_at, name, location) VALUES
(NOW(), NOW(), 'Main Warehouse', 'New York'),
(NOW(), NOW(), 'West Coast Hub', 'Los Angeles'),
(NOW(), NOW(), 'European Distribution', 'Berlin')
ON CONFLICT (name) DO NOTHING;

-- Insert Items
INSERT INTO items (created_at, updated_at, code, name, description, price, uom) VALUES
(NOW(), NOW(), 'ITEM-001', 'Steel Sheet', 'Standard steel sheet 2mm', 50.00, 'SHEET'),
(NOW(), NOW(), 'ITEM-002', 'Electronic Circuit Board', 'Main control board', 120.00, 'UNIT'),
(NOW(), NOW(), 'ITEM-003', 'Plastic Casing', 'Durable plastic casing', 15.00, 'UNIT'),
(NOW(), NOW(), 'PROD-001', 'Smart Widget', 'Finished smart widget product', 350.00, 'UNIT')
ON CONFLICT (code) DO NOTHING;

-- Insert BOMs (Bill of Materials) for PROD-001
INSERT INTO boms (created_at, updated_at, item_id, name, description) 
SELECT NOW(), NOW(), id, 'BOM-PROD-001', 'Standard BOM for Smart Widget'
FROM items 
WHERE code = 'PROD-001'
AND NOT EXISTS (SELECT 1 FROM boms WHERE name = 'BOM-PROD-001');

-- Insert BOM Components
INSERT INTO bom_components (created_at, updated_at, bom_id, item_id, quantity)
SELECT NOW(), NOW(), b.id, i.id, 1
FROM boms b, items i
WHERE b.name = 'BOM-PROD-001' AND i.code = 'ITEM-002'
AND NOT EXISTS (SELECT 1 FROM bom_components bc WHERE bc.bom_id = b.id AND bc.item_id = i.id);

INSERT INTO bom_components (created_at, updated_at, bom_id, item_id, quantity)
SELECT NOW(), NOW(), b.id, i.id, 1
FROM boms b, items i
WHERE b.name = 'BOM-PROD-001' AND i.code = 'ITEM-003'
AND NOT EXISTS (SELECT 1 FROM bom_components bc WHERE bc.bom_id = b.id AND bc.item_id = i.id);

INSERT INTO bom_components (created_at, updated_at, bom_id, item_id, quantity)
SELECT NOW(), NOW(), b.id, i.id, 2
FROM boms b, items i
WHERE b.name = 'BOM-PROD-001' AND i.code = 'ITEM-001'
AND NOT EXISTS (SELECT 1 FROM bom_components bc WHERE bc.bom_id = b.id AND bc.item_id = i.id);

-- Insert PRODUCTION_SUPERVISOR role
INSERT INTO roles (id, name, label, css_class)
SELECT COALESCE((SELECT MAX(id) + 1 FROM roles), 8), 'PRODUCTION_SUPERVISOR', 'Üretim Sorumlusu', 'primary'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'PRODUCTION_SUPERVISOR');

-- Insert PRODUCTION_SUPERVISOR demo user
INSERT INTO users (id, username, full_name, email, password, role_id, active, created_at)
SELECT
    COALESCE((SELECT MAX(id) + 1 FROM users), 6),
    'productionsupervisor',
    'Üretim Sorumlusu',
    'productionsupervisor@erp.com',
    '$2a$10$N9qo8uLOickgm2kzhZLsqdXPwyeG5n1Bk1J3BwvI7X5GJlL1J3Bw2', -- password123 hash
    COALESCE((SELECT id FROM roles WHERE name = 'PRODUCTION_SUPERVISOR'), 8),
    true,
    NOW() - INTERVAL '50 days'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'productionsupervisor');

-- Insert demo Operations for Production Supervisor
INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, work_center, capacity, capacity_unit, active)
SELECT
    NOW(), NOW(), 'OP-CUT', 'Kesim Operasyonu', 'Malzeme kesim işlemi için standart operasyon', 30, 'MINUTES', 1, 'Makine 1', 100, 'Adet/Saat', true
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-CUT');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, work_center, capacity, capacity_unit, active)
SELECT
    NOW(), NOW(), 'OP-MACH', 'Torna Operasyonu', 'Malzeme tornalama işlemi için standart operasyon', 45, 'MINUTES', 2, 'Torna 1', 50, 'Adet/Saat', true
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-MACH');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, work_center, capacity, capacity_unit, active)
SELECT
    NOW(), NOW(), 'OP-ASM', 'Montaj Operasyonu', 'Parça montaj işlemi için standart operasyon', 60, 'MINUTES', 3, 'Montaj 1', 30, 'Adet/Saat', true
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-ASM');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, work_center, capacity, capacity_unit, active)
SELECT
    NOW(), NOW(), 'OP-QC', 'Kalite Kontrol Operasyonu', 'Ürün kalite kontrolü için standart operasyon', 15, 'MINUTES', 4, 'QC 1', 20, 'Adet/Saat', true
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-QC');

-- Insert Operation Material Requirements for demo operations
-- OP-CUT requirements
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, item_name, required_quantity, unit, scrap_rate, is_critical, description)
SELECT
    NOW(), NOW(), o.id, i.id, i.name, 1.5, 'Adet', 5.0, true, 'Kesim esnasında birim başına 5% atık'
FROM operations o, items i
WHERE o.code = 'OP-CUT' AND i.code = 'ITEM-001'
AND NOT EXISTS (SELECT 1 FROM operation_material_requirements omr
    WHERE omr.operation_id = o.id AND omr.item_id = i.id);

-- OP-MACH requirements
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, item_name, required_quantity, unit, scrap_rate, is_critical, description)
SELECT
    NOW(), NOW(), o.id, i.id, i.name, 0.8, 'Adet', 2.0, false, 'Torna esnasında birim başına 2% atık'
FROM operations o, items i
WHERE o.code = 'OP-MACH' AND i.code = 'ITEM-001'
AND NOT EXISTS (SELECT 1 FROM operation_material_requirements omr
    WHERE omr.operation_id = o.id AND omr.item_id = i.id);

-- OP-ASM requirements
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, item_name, required_quantity, unit, scrap_rate, is_critical, description)
SELECT
    NOW(), NOW(), o.id, i.id, i.name, 1.0, 'Adet', 0.0, true, 'Montaj malzemesi - kritik'
FROM operations o, items i
WHERE o.code = 'OP-ASM' AND i.code = 'ITEM-002'
AND NOT EXISTS (SELECT 1 FROM operation_material_requirements omr
    WHERE omr.operation_id = o.id AND omr.item_id = i.id);

INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, item_name, required_quantity, unit, scrap_rate, is_critical, description)
SELECT
    NOW(), NOW(), o.id, i.id, i.name, 2.0, 'Adet', 1.0, false, 'Plastik kapak sayısı'
FROM operations o, items i
WHERE o.code = 'OP-ASM' AND i.code = 'ITEM-003'
AND NOT EXISTS (SELECT 1 FROM operation_material_requirements omr
    WHERE omr.operation_id = o.id AND omr.item_id = i.id);
