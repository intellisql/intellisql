-- @case id=dql-customer-select model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=strict
-- @statement mode=statement
SELECT id, customer_name, status FROM customers ORDER BY id;
