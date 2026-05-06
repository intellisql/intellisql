-- @case id=mysql-orders-subquery-in model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_id FROM mysql_orders WHERE customer_id IN (SELECT customer_id FROM mysql_customers WHERE region = 'NA') ORDER BY order_id;
