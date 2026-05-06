-- @case id=mysql-orders-left-join model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_name FROM mysql_orders LEFT JOIN mysql_customers USING (customer_id) ORDER BY order_id;
