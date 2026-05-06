-- @case id=mysql-orders-select model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_id, status, amount FROM mysql_orders ORDER BY order_id;
