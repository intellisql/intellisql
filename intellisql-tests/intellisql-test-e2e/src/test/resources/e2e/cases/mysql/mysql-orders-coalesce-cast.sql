-- @case id=mysql-orders-coalesce-cast model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT order_id, COALESCE(note, 'missing') AS note_value, CAST(amount AS CHAR(2)) AS amount_text FROM mysql_orders ORDER BY order_id;
