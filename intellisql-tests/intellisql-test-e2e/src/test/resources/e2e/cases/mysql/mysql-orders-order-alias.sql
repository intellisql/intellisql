-- @case id=mysql-orders-order-alias model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount AS order_amount FROM mysql_orders ORDER BY order_amount DESC, order_id;
