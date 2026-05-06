-- @case id=dql-customer-filter model=basic
-- @source intellisql
-- @assert mirror target=postgresql order=any
-- @statement mode=statement
SELECT id, customer_name FROM customers WHERE status = 'ACTIVE';
