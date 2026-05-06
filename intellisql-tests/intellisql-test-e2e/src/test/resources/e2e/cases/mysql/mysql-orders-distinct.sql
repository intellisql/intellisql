-- @case id=mysql-orders-distinct model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT DISTINCT customer_id FROM mysql_orders ORDER BY customer_id;
