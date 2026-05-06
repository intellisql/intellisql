-- @case id=mysql-orders-join-on model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, customer_name FROM mysql_orders JOIN mysql_customers ON mysql_orders.customer_id = mysql_customers.customer_id ORDER BY order_id;
