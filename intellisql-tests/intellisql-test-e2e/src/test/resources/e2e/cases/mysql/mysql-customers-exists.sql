-- @case id=mysql-customers-exists model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id FROM mysql_customers WHERE EXISTS (SELECT 1 FROM mysql_orders WHERE status = 'PAID') ORDER BY customer_id;
