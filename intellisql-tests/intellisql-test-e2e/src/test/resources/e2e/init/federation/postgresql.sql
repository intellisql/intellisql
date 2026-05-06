DROP TABLE IF EXISTS pg_customers;
CREATE TABLE pg_customers (
    id VARCHAR(16) PRIMARY KEY,
    customer_name VARCHAR(64) NOT NULL
);
INSERT INTO pg_customers (id, customer_name) VALUES
('P001', 'Postgres Alice'),
('P002', 'Postgres Bob');
