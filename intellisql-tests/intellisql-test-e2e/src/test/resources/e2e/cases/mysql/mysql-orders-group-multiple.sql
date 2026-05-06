-- @case id=mysql-orders-group-multiple model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, status, COUNT(*) AS order_count FROM mysql_orders GROUP BY customer_id, status ORDER BY customer_id, status;
