-- @case id=mysql-orders-case-expression model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, CASE WHEN note IS NULL THEN 'NONE' ELSE note END AS note_label FROM mysql_orders ORDER BY order_id;
