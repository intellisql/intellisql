-- @case id=mysql-orders-recursive-cte model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
WITH RECURSIVE order_numbers(level_value) AS (
    SELECT 1
    UNION ALL
    SELECT level_value + 1 FROM order_numbers WHERE level_value < 3
)
SELECT level_value FROM order_numbers ORDER BY level_value;
