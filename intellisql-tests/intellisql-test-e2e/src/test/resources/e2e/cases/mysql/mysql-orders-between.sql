-- @case id=mysql-orders-between model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE amount BETWEEN 15 AND 40 ORDER BY amount;
