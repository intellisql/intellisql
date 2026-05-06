-- @case id=mysql-orders-cross-join model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_name FROM mysql_orders CROSS JOIN mysql_customers ORDER BY order_id, customer_name;
