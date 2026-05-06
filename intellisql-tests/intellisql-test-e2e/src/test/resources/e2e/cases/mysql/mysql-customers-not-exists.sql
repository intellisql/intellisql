-- @case id=mysql-customers-not-exists model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, customer_name FROM mysql_customers WHERE NOT EXISTS (SELECT 1 FROM mysql_orders WHERE mysql_orders.customer_id = mysql_customers.customer_id AND status = 'PAID') ORDER BY customer_id;
