-- @case id=federation-pg-customers model=federation
-- @source intellisql
-- @assert file expected=expected/federation/pg-customers.csv order=strict
-- @statement mode=statement
SELECT id, customer_name FROM pg_customers ORDER BY id;
