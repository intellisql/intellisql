-- @case id=mysql-orders-natural-join model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id, order_id, customer_name FROM mysql_orders NATURAL JOIN mysql_customers ORDER BY order_id;
