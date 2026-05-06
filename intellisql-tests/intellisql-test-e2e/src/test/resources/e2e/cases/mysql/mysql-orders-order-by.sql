-- @case id=mysql-orders-order-by model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=auto
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY amount DESC;
