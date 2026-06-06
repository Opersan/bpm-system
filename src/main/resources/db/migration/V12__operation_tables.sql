-- Production Module - Operation Tables (V12)

CREATE TABLE IF NOT EXISTS operations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    standard_duration DECIMAL(19, 4) NOT NULL DEFAULT 0,
    duration_unit VARCHAR(20) NOT NULL DEFAULT 'MINUTES',
    default_sequence INTEGER NOT NULL DEFAULT 1,
    work_center VARCHAR(100),
    capacity DECIMAL(19, 4),
    capacity_unit VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS operation_material_requirements (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    operation_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(200),
    required_quantity DECIMAL(19, 4) NOT NULL,
    unit VARCHAR(50),
    scrap_rate DECIMAL(5, 2) DEFAULT 0,
    is_critical BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    CONSTRAINT fk_op_mat_op FOREIGN KEY (operation_id) REFERENCES operations (id),
    CONSTRAINT fk_op_mat_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE INDEX IF NOT EXISTS idx_operation_material_req_op ON operation_material_requirements (operation_id);
CREATE INDEX IF NOT EXISTS idx_operation_material_req_item ON operation_material_requirements (item_id);

CREATE TABLE IF NOT EXISTS work_order_operations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    work_order_id BIGINT NOT NULL,
    operation_id BIGINT NOT NULL,
    sequence_number INTEGER NOT NULL,
    planned_duration DECIMAL(19, 4) NOT NULL DEFAULT 0,
    duration_unit VARCHAR(20) NOT NULL DEFAULT 'MINUTES',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    CONSTRAINT fk_wo_op_wo FOREIGN KEY (work_order_id) REFERENCES work_orders (id),
    CONSTRAINT fk_wo_op_op FOREIGN KEY (operation_id) REFERENCES operations (id)
);

CREATE INDEX IF NOT EXISTS idx_work_order_op_wo ON work_order_operations (work_order_id);
CREATE INDEX IF NOT EXISTS idx_work_order_op_op ON work_order_operations (operation_id);
CREATE INDEX IF NOT EXISTS idx_work_order_op_seq ON work_order_operations (work_order_id, sequence_number);

CREATE TABLE IF NOT EXISTS work_order_material_requirements (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    work_order_operation_id BIGINT NOT NULL,
    operation_material_requirement_id BIGINT,
    item_id BIGINT NOT NULL,
    unit VARCHAR(50) NOT NULL,
    standard_quantity_per_operation DECIMAL(19, 4) NOT NULL,
    total_required_quantity DECIMAL(19, 4) NOT NULL,
    scrap_rate DECIMAL(5, 2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    description TEXT,
    CONSTRAINT fk_wo_mat_wo_op FOREIGN KEY (work_order_operation_id) REFERENCES work_order_operations (id),
    CONSTRAINT fk_wo_mat_op_mat FOREIGN KEY (operation_material_requirement_id) REFERENCES operation_material_requirements (id),
    CONSTRAINT fk_wo_mat_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE INDEX IF NOT EXISTS idx_work_order_mat_req_wo_op ON work_order_material_requirements (work_order_operation_id);
CREATE INDEX IF NOT EXISTS idx_work_order_mat_req_item ON work_order_material_requirements (item_id);
CREATE INDEX IF NOT EXISTS idx_work_order_mat_req_wo ON work_order_material_requirements (work_order_operation_id);

-- Insert sample operations for demo data
INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, active)
SELECT NOW(), NOW(), 'OP-CUT', 'Kesim Operasyonu', 'Malzeme kesim işlemi', 30, 'MINUTES', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-CUT');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, active)
SELECT NOW(), NOW(), 'OP-MACH', 'Torna Operasyonu', 'Parça tornalama işlemi', 45, 'MINUTES', 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-MACH');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, active)
SELECT NOW(), NOW(), 'OP-ASM', 'Montaj Operasyonu', 'Parça montaj işlemi', 60, 'MINUTES', 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-ASM');

INSERT INTO operations (created_at, updated_at, code, name, description, standard_duration, duration_unit, default_sequence, active)
SELECT NOW(), NOW(), 'OP-QC', 'Kalite Kontrol', 'Ürün kalite kontrolü', 15, 'MINUTES', 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM operations WHERE code = 'OP-QC');

-- Insert sample material requirements for operations
-- For OP-CUT (Kesim), need steel sheet
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, required_quantity, unit, is_critical)
SELECT NOW(), NOW(), 
    (SELECT id FROM operations WHERE code = 'OP-CUT'),
    (SELECT id FROM items WHERE code = 'ITEM-001'),
    1.0, 'SHEET', TRUE
WHERE EXISTS (SELECT 1 FROM operations WHERE code = 'OP-CUT')
  AND EXISTS (SELECT 1 FROM items WHERE code = 'ITEM-001');

-- For OP-MACH (Torna), need steel sheet
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, required_quantity, unit, is_critical)
SELECT NOW(), NOW(), 
    (SELECT id FROM operations WHERE code = 'OP-MACH'),
    (SELECT id FROM items WHERE code = 'ITEM-001'),
    0.8, 'SHEET', TRUE
WHERE EXISTS (SELECT 1 FROM operations WHERE code = 'OP-MACH')
  AND EXISTS (SELECT 1 FROM items WHERE code = 'ITEM-001');

-- For OP-ASM (Montaj), need circuit board and casing
INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, required_quantity, unit, is_critical)
SELECT NOW(), NOW(), 
    (SELECT id FROM operations WHERE code = 'OP-ASM'),
    (SELECT id FROM items WHERE code = 'ITEM-002'),
    1.0, 'UNIT', TRUE
WHERE EXISTS (SELECT 1 FROM operations WHERE code = 'OP-ASM')
  AND EXISTS (SELECT 1 FROM items WHERE code = 'ITEM-002');

INSERT INTO operation_material_requirements (created_at, updated_at, operation_id, item_id, required_quantity, unit, is_critical)
SELECT NOW(), NOW(), 
    (SELECT id FROM operations WHERE code = 'OP-ASM'),
    (SELECT id FROM items WHERE code = 'ITEM-003'),
    1.0, 'UNIT', FALSE
WHERE EXISTS (SELECT 1 FROM operations WHERE code = 'OP-ASM')
  AND EXISTS (SELECT 1 FROM items WHERE code = 'ITEM-003');

-- For OP-QC (Kalite Kontrol), no material requirement needed
