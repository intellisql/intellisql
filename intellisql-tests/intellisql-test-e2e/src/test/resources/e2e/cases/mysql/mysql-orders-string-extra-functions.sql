-- @case id=mysql-orders-string-extra-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, LEFT(status, 2) AS status_left, RIGHT(status, 2) AS status_right, REPLACE(COALESCE(note, 'missing order'), 'order', 'item') AS replaced_note FROM mysql_orders ORDER BY order_id;
