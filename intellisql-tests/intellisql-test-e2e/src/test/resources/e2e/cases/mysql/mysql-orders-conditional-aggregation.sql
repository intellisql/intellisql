-- @case id=mysql-orders-conditional-aggregation model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END) AS paid_amount FROM mysql_orders GROUP BY customer_id ORDER BY customer_id;
