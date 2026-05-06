-- @case id=mysql-orders-not-predicates model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, status FROM mysql_orders WHERE status NOT IN ('PAID') AND order_id NOT LIKE 'M004' ORDER BY order_id;
