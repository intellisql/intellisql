-- @case id=mysql-orders-regexp-like model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, status FROM mysql_orders WHERE REGEXP_LIKE(status, 'PAID|PENDING') ORDER BY order_id;
