-- Demo data for local H2 development
-- Runs on every startup but all inserts are idempotent (ON CONFLICT DO NOTHING)

-- Suppliers
INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'TechComponents Ltd', 'sales@techcomponents.com', '123 Tech Park, Silikon Vadisi'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'TechComponents Ltd');

INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'MetalWorks Inc', 'orders@metalworks.com', '456 Sanayi Cad., İstanbul'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'MetalWorks Inc');

INSERT INTO suppliers (created_at, updated_at, name, contact_email, address)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Global Lojistik A.Ş.', 'contact@globallogistics.com', '789 Liman Yolu, İzmir'
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name = 'Global Lojistik A.Ş.');

-- Warehouses
MERGE INTO warehouses (name, location, created_at, updated_at)
KEY(name)
VALUES ('Ana Depo', 'İstanbul', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO warehouses (name, location, created_at, updated_at)
KEY(name)
VALUES ('Batı Deposu', 'İzmir', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Items
MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('ITEM-001', 'Çelik Levha', 'Standart çelik levha 2mm', 50.00, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('ITEM-002', 'Elektronik Devre Kartı', 'Ana kontrol kartı', 120.00, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('ITEM-003', 'Plastik Gövde', 'Dayanıklı plastik gövde', 15.00, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('PROD-001', 'Akıllı Widget', 'Bitmiş akıllı widget ürünü', 350.00, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('VD-M6-50', 'Vida M6x50', 'Paslanmaz çelik vida', 0.50, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('SM-M6', 'Altıköşe Somun M6', 'Galvanizli somun', 0.30, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO items (code, name, description, price, uom, created_at, updated_at)
KEY(code)
VALUES ('ZM-P120', 'Zımpara Kağıdı P120', '230x280mm boyutunda', 2.50, 'ADET', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample Purchase Requests
INSERT INTO purchase_requests (created_at, updated_at, request_number, description, department, requested_by, required_by, status)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PT-20260522-0001', 'Vida ve cıvata ihtiyacı - Montaj hattı için acil', 'Üretim', 'production', '2026-06-01', 'SUBMITTED'
WHERE NOT EXISTS (SELECT 1 FROM purchase_requests WHERE request_number = 'PT-20260522-0001');

INSERT INTO purchase_requests (created_at, updated_at, request_number, description, department, requested_by, required_by, status)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PT-20260522-0002', 'Endüstriyel zımpara kağıdı temin talebi', 'Üretim', 'production', '2026-06-10', 'SUBMITTED'
WHERE NOT EXISTS (SELECT 1 FROM purchase_requests WHERE request_number = 'PT-20260522-0002');

-- Purchase Request Items
INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, pr.id, 'Vida M6x50', 'VD-M6-50', 500, 'ADET', 'Paslanmaz çelik olması tercih edilir'
FROM purchase_requests pr
WHERE pr.request_number = 'PT-20260522-0001'
AND NOT EXISTS (SELECT 1 FROM purchase_request_items WHERE purchase_request_id = pr.id AND item_code = 'VD-M6-50');

INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, pr.id, 'Altıköşe Somun M6', 'SM-M6', 500, 'ADET', NULL
FROM purchase_requests pr
WHERE pr.request_number = 'PT-20260522-0001'
AND NOT EXISTS (SELECT 1 FROM purchase_request_items WHERE purchase_request_id = pr.id AND item_code = 'SM-M6');

INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, pr.id, 'Zımpara Kağıdı P120', 'ZM-P120', 100, 'ADET', '230x280mm boyutunda'
FROM purchase_requests pr
WHERE pr.request_number = 'PT-20260522-0002'
AND NOT EXISTS (SELECT 1 FROM purchase_request_items WHERE purchase_request_id = pr.id AND item_code = 'ZM-P120');
