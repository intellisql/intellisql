-- @case id=mysql-orders-timestampdiff model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, DATEDIFF(DATE_ADD(created_on, INTERVAL 3 DAY), created_on) AS day_gap, TIMESTAMPDIFF(DAY, created_on, DATE_ADD(created_on, INTERVAL 3 DAY)) AS timestamp_day_gap FROM mysql_orders ORDER BY order_id;
