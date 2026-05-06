-- @case id=mysql-orders-derived-table model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM (SELECT order_id, amount FROM mysql_orders WHERE amount >= 20) AS filtered_orders ORDER BY order_id;
