-- @case id=mysql-orders-limit model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=auto
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY order_id LIMIT 2;
