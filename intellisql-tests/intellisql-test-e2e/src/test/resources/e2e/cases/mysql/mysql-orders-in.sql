-- @case id=mysql-orders-in model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, status FROM mysql_orders WHERE status IN ('PAID', 'PENDING') ORDER BY order_id;
