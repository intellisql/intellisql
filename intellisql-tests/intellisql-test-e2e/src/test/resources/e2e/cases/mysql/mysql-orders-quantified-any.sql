-- @case id=mysql-orders-quantified-any model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE amount > ANY (SELECT amount FROM mysql_orders WHERE status = 'PAID') ORDER BY amount, order_id;
