-- @case id=mysql-orders-concat-date-extract model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, CONCAT(customer_id, '-', order_id) AS order_key, EXTRACT(DAY FROM created_on) AS created_day FROM mysql_orders ORDER BY order_id;
