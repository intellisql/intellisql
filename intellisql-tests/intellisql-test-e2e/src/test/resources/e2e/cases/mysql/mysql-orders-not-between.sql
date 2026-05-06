-- @case id=mysql-orders-not-between model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE amount NOT BETWEEN 20 AND 30 ORDER BY amount;
