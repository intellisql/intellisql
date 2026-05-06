-- @case id=mysql-orders-count-distinct model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=auto
-- @statement mode=statement
SELECT COUNT(DISTINCT customer_id) AS customer_count FROM mysql_orders;
