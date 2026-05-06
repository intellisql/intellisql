-- @case id=mysql-orders-cte model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
WITH paid_orders AS (SELECT order_id, amount FROM mysql_orders WHERE status = 'PAID')
SELECT order_id, amount FROM paid_orders ORDER BY order_id;
