-- @case id=mysql-orders-star model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT * FROM mysql_orders ORDER BY order_id;
