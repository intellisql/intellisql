-- @case id=mysql-orders-expressions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount + 5 AS amount_plus_five, amount * 2 AS amount_double FROM mysql_orders ORDER BY order_id;
