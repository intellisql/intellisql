-- @case id=mysql-orders-filter model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=any
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE status = 'PAID';
