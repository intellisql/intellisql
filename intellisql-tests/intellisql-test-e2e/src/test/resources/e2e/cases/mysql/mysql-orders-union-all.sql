-- @case id=mysql-orders-union-all model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id AS item_id, 'orders' AS item_type FROM mysql_orders WHERE amount <= 20
UNION ALL
SELECT customer_id AS item_id, 'client' AS item_type FROM mysql_customers WHERE priority <= 2
ORDER BY item_id, item_type;
