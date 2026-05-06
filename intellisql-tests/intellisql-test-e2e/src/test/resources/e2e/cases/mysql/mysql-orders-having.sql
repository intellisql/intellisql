-- @case id=mysql-orders-having model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, SUM(amount) AS total_amount FROM mysql_orders GROUP BY customer_id HAVING SUM(amount) >= 40 ORDER BY customer_id;
