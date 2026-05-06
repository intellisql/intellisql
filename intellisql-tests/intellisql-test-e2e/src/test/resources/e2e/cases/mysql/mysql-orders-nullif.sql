-- @case id=mysql-orders-nullif model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, NULLIF(status, 'PAID') AS non_paid_status FROM mysql_orders ORDER BY order_id;
