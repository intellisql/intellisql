-- @case id=mysql-orders-window-aggregate-frame model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_id, SUM(amount) OVER (PARTITION BY customer_id ORDER BY created_on ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_amount FROM mysql_orders ORDER BY customer_id, order_id;
