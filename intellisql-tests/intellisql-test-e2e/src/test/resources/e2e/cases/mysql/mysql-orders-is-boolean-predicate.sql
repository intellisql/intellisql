-- @case id=mysql-orders-is-boolean-predicate model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE (amount > 20) IS TRUE ORDER BY order_id;
