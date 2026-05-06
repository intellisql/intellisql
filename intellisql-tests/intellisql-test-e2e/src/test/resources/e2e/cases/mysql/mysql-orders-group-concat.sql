-- @case id=mysql-orders-group-concat model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, GROUP_CONCAT(order_id ORDER BY order_id SEPARATOR ',') AS order_ids FROM mysql_orders GROUP BY customer_id ORDER BY customer_id;
