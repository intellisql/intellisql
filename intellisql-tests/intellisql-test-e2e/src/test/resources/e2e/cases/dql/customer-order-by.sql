-- @case id=dql-customer-order-by model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=auto
-- @statement mode=statement
SELECT id, customer_name FROM customers ORDER BY customer_name DESC;
