-- @case id=mysql-orders-inner-join model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_name, amount FROM mysql_orders JOIN mysql_customers USING (customer_id) ORDER BY order_id;
