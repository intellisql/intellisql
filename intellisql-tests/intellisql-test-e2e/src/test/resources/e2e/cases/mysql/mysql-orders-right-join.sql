-- @case id=mysql-orders-right-join model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, order_id, customer_name FROM mysql_orders RIGHT JOIN mysql_customers USING (customer_id) ORDER BY customer_id, order_id;
