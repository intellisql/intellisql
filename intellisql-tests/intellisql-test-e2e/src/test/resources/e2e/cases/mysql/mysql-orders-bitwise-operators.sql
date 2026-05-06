-- @case id=mysql-orders-bitwise-operators model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, amount & 3 AS bit_and_value, amount | 1 AS bit_or_value, amount ^ 2 AS bit_xor_value FROM mysql_orders ORDER BY order_id;
