-- @case id=mysql-orders-order-ordinal model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY 2 DESC, 1 ASC;
