-- @case id=mysql-orders-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, LOWER(status) AS lower_status, UPPER(customer_id) AS upper_customer_id FROM mysql_orders ORDER BY order_id;
