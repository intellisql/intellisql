DROP TABLE IF EXISTS mysql_orders;
DROP TABLE IF EXISTS mysql_customers;
CREATE TABLE mysql_customers (
    customer_id VARCHAR(16) PRIMARY KEY,
    customer_name VARCHAR(32) NOT NULL,
    region VARCHAR(16) NOT NULL,
    customer_status VARCHAR(16) NOT NULL,
    priority INT NOT NULL
);
CREATE TABLE mysql_orders (
    order_id VARCHAR(16) PRIMARY KEY,
    customer_id VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount INT NOT NULL,
    created_on DATE NOT NULL,
    note VARCHAR(64)
);
INSERT INTO mysql_customers (customer_id, customer_name, region, customer_status, priority) VALUES
('C001', 'Alice', 'NA', 'ACTIVE', 1),
('C002', 'Bob', 'EU', 'ACTIVE', 2);
INSERT INTO mysql_orders (order_id, customer_id, status, amount, created_on, note) VALUES
('M001', 'C001', 'PAID', 10, '2026-01-01', 'first order'),
('M002', 'C002', 'PENDING', 20, '2026-01-02', NULL),
('M003', 'C001', 'PAID', 30, '2026-01-03', 'repeat order'),
('M004', 'C003', 'CANCELLED', 40, '2026-01-04', NULL);
