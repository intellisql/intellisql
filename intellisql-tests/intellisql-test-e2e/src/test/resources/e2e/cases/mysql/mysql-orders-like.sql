-- @case id=mysql-orders-like model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_id FROM mysql_orders WHERE order_id LIKE 'M00%' AND customer_id <> 'C003' ORDER BY order_id;
