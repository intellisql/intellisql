-- @case id=mysql-orders-limit-offset-comma model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY amount LIMIT 1, 2;
