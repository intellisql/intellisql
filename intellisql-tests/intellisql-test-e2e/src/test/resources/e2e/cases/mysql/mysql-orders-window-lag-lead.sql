-- @case id=mysql-orders-window-lag-lead model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, LAG(amount, 1, 0) OVER (ORDER BY amount) AS previous_amount, LEAD(amount, 1, 0) OVER (ORDER BY amount) AS next_amount FROM mysql_orders ORDER BY amount;
