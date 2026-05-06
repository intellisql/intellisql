-- @case id=dml-customer-update model=basic
-- @source intellisql
-- @assert state target=postgresql order=strict
-- @statement mode=statement
-- @expected-sql SELECT id, status FROM customers WHERE id = 'C002' ORDER BY id
UPDATE customers SET status = 'ACTIVE' WHERE id = 'C002';
