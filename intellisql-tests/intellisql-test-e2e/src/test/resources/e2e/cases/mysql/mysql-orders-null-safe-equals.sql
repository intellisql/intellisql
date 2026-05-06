-- @case id=mysql-orders-null-safe-equals model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, note FROM mysql_orders WHERE note <=> NULL ORDER BY order_id;
