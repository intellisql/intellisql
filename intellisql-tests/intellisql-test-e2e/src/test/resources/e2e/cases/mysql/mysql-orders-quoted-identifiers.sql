-- @case id=mysql-orders-quoted-identifiers model=mysql
-- @source intellisql
-- @assert mirror target=mysql order=strict
-- @statement mode=statement
SELECT `order_id` AS quoted_order_id, `amount` AS quoted_amount FROM `mysql_orders` ORDER BY `order_id`;
