-- @case id=mysql-orders-aggregate-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=auto
-- @statement mode=statement
SELECT COUNT(*) AS order_count, SUM(amount) AS total_amount, AVG(amount) AS avg_amount, MIN(amount) AS min_amount, MAX(amount) AS max_amount FROM mysql_orders;
