-- @case id=mysql-orders-multi-order model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, status, amount FROM mysql_orders ORDER BY status ASC, amount DESC;
