-- @case id=mysql-orders-having-count model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, COUNT(*) AS order_count FROM mysql_orders GROUP BY customer_id HAVING COUNT(*) >= 2 ORDER BY customer_id;
