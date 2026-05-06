-- @case id=mysql-orders-comparison-operators model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount FROM mysql_orders WHERE amount > 10 AND amount <= 40 AND status <> 'CANCELLED' ORDER BY amount;
