-- Purchase Request Workflow Tables
-- V9: Satın Alma Talebi (Purchase Request) Workflow

CREATE TABLE purchase_requests (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    request_number VARCHAR(30) NOT NULL UNIQUE,
    description TEXT,
    department VARCHAR(100),
    requested_by VARCHAR(100) NOT NULL,
    required_by DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    handled_by VARCHAR(100),
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    purchase_order_id BIGINT,
    CONSTRAINT fk_pr_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id)
);

CREATE TABLE purchase_request_items (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    purchase_request_id BIGINT NOT NULL,
    item_id BIGINT,
    item_name VARCHAR(255) NOT NULL,
    item_code VARCHAR(50),
    quantity DECIMAL(19, 2) NOT NULL,
    uom VARCHAR(20),
    notes TEXT,
    CONSTRAINT fk_pri_pr FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests (id),
    CONSTRAINT fk_pri_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE TABLE purchase_request_quotes (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    purchase_request_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    estimated_delivery_date DATE,
    notes TEXT,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prq_pr FOREIGN KEY (purchase_request_id) REFERENCES purchase_requests (id),
    CONSTRAINT fk_prq_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- Demo data: sample purchase requests
INSERT INTO purchase_requests (created_at, updated_at, request_number, description, department, requested_by, required_by, status)
VALUES
    (NOW(), NOW(), 'PT-20260522-0001', 'Vida ve cıvata ihtiyacı - Montaj hattı için acil', 'Üretim', 'production', '2026-06-01', 'SUBMITTED'),
    (NOW(), NOW(), 'PT-20260522-0002', 'Endüstriyel zımpara kağıdı temin talebi', 'Üretim', 'production', '2026-06-10', 'SUBMITTED');

-- Demo request items
INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT NOW(), NOW(), id, 'Vida M6x50', 'VD-M6-50', 500, 'EA', 'Paslanmaz çelik olması tercih edilir'
FROM purchase_requests WHERE request_number = 'PT-20260522-0001';

INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT NOW(), NOW(), id, 'Altıköşe Somun M6', 'SM-M6', 500, 'EA', NULL
FROM purchase_requests WHERE request_number = 'PT-20260522-0001';

INSERT INTO purchase_request_items (created_at, updated_at, purchase_request_id, item_name, item_code, quantity, uom, notes)
SELECT NOW(), NOW(), id, 'Zımpara Kağıdı P120', 'ZM-P120', 100, 'EA', '230x280mm boyutunda'
FROM purchase_requests WHERE request_number = 'PT-20260522-0002';
