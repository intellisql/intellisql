-- @case id=mysql-orders-simple-case model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, CASE status WHEN 'PAID' THEN 1 WHEN 'PENDING' THEN 2 ELSE 3 END AS status_code FROM mysql_orders ORDER BY order_id;
