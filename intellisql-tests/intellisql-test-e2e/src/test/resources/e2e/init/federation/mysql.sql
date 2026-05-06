DROP TABLE IF EXISTS mysql_orders;
CREATE TABLE mysql_orders (
    order_id VARCHAR(16) PRIMARY KEY,
    amount INT NOT NULL
);
INSERT INTO mysql_orders (order_id, amount) VALUES
('M001', 10),
('M002', 20);
