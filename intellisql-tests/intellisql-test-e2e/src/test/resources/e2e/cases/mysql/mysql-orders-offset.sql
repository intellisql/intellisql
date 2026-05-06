-- @case id=mysql-orders-offset model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY amount LIMIT 2 OFFSET 1;
