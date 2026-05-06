-- @case id=mysql-orders-string-pad-locate model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, LPAD(customer_id, 5, '0') AS padded_customer, LOCATE('0', order_id) AS zero_position FROM mysql_orders ORDER BY order_id;
