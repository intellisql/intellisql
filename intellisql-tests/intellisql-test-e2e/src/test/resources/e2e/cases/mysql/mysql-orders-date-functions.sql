-- @case id=mysql-orders-date-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, DATE_ADD(created_on, INTERVAL 2 DAY) AS due_on, DATE_SUB(created_on, INTERVAL 1 DAY) AS previous_on FROM mysql_orders ORDER BY order_id;
