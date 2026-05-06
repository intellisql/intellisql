-- @case id=mysql-orders-boolean model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, status, amount FROM mysql_orders WHERE (status = 'PAID' AND amount >= 10) OR status = 'CANCELLED' ORDER BY order_id;
