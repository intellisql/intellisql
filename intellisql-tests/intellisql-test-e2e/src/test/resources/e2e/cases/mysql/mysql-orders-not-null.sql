-- @case id=mysql-orders-not-null model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, note FROM mysql_orders WHERE note IS NOT NULL ORDER BY order_id;
