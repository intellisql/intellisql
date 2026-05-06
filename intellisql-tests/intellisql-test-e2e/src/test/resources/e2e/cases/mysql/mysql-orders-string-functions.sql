-- @case id=mysql-orders-string-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, SUBSTRING(status FROM 1 FOR 1) AS status_prefix, CHAR_LENGTH(order_id) AS id_length FROM mysql_orders ORDER BY order_id;
