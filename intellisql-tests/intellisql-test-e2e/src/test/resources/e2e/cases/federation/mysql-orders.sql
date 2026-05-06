-- @case id=federation-mysql-orders model=federation
-- @source intellisql
-- @assert file expected=expected/federation/mysql-orders.csv order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders ORDER BY order_id;
