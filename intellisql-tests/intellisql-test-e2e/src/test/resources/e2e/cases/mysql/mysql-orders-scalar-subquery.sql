-- @case id=mysql-orders-scalar-subquery model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE amount > (SELECT AVG(amount) FROM mysql_orders) ORDER BY order_id;
