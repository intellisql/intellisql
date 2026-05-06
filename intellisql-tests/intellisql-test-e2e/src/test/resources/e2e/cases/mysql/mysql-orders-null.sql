-- @case id=mysql-orders-null model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, note FROM mysql_orders WHERE note IS NULL ORDER BY order_id;
