-- @case id=mysql-orders-date model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, created_on FROM mysql_orders WHERE created_on >= DATE '2026-01-02' ORDER BY created_on;
