-- @case id=mysql-orders-aggregate model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=auto
-- @statement mode=statement
SELECT status, COUNT(*) AS order_count FROM mysql_orders GROUP BY status ORDER BY status;
