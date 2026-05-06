-- @case id=smoke-show-tables model=basic
-- @source intellisql
-- @assert file expected=expected/smoke/show-tables.csv order=strict
-- @statement mode=statement
SELECT tablename FROM e2e_table_names ORDER BY tablename;
