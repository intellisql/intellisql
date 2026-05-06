-- @case id=mysql-orders-window-row-number model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, ROW_NUMBER() OVER (ORDER BY amount) AS row_number_value FROM mysql_orders ORDER BY order_id;
