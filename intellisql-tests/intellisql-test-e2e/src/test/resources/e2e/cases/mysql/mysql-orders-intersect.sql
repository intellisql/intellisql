-- @case id=mysql-orders-intersect model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT customer_id AS item_id FROM mysql_orders
INTERSECT
SELECT customer_id AS item_id FROM mysql_customers
ORDER BY item_id;
