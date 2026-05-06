-- @case id=mysql-orders-numeric-functions model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, ABS(amount - 25) AS distance_value, MOD(amount, 3) AS remainder_value, ROUND(amount / 3.0, 2) AS rounded_value FROM mysql_orders ORDER BY order_id;
