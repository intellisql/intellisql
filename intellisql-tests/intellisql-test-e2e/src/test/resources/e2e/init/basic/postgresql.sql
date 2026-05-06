DROP TABLE IF EXISTS e2e_table_names;
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
    id VARCHAR(16) PRIMARY KEY,
    customer_name VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    score VARCHAR(16) NOT NULL
);
INSERT INTO customers (id, customer_name, status, score) VALUES
('C001', 'Alice', 'ACTIVE', '90'),
('C002', 'Bob', 'INACTIVE', '70'),
('C003', 'Carol', 'ACTIVE', '95');
CREATE TABLE e2e_table_names (
    tablename VARCHAR(64) PRIMARY KEY
);
INSERT INTO e2e_table_names (tablename) VALUES
('customers'),
('e2e_table_names');
